(ns pivot.engines.druid
  (:require [clj-http.client :as http]
            [pivot.engines.engine :as e :refer [DwEngine]]
            [pivot.lib.collections :refer [detect]]
            [pivot.lib.logging :refer [pp-str]]
            [clojure.string :as str]
            [taoensso.timbre :as log]))

; TODO http-kit viene con un client, ver si no se puede usar para no tener q agregar otra dependencia
(defn datasources [host]
  (->> (http/get (str host "/druid/v2/datasources") {:as :json :conn-timeout 10000})
       :body (map (fn [name] {:name name}))))

; TODO en el :context de la q se le puede pasar un timeout
; TODO ver como hacer para no loggear en testing
(defn- send-query [host q]
  (log/debug "Sending query to druid:\n" (pp-str q))
  (:body (http/post (str host "/druid/v2") {:content-type :json :form-params q :as :json})))

(defn- segment-metadata-query [host datasource]
  (let [q {:queryType "segmentMetadata"
           :dataSource {:type "table" :name datasource}
           :merge true
           :analysisTypes ["cardinality" "aggregators"]
           :intervals ["2000/2100"]
           :lenientAggregatorMerge true}]
    (first (send-query host q))))

(defn- druid-column-result-to-map [[column fields]]
  (merge (select-keys fields [:type :cardinality])
         {:name (name column)}))

(defn- no-metric-columns [{:keys [columns aggregators] :as result}]
  (remove #(some #{(first %)} (keys aggregators)) columns))

(defn dimensions [host datasource]
  (->> (segment-metadata-query host datasource)
       no-metric-columns
       (map druid-column-result-to-map)))

(defn metrics [host datasource]
  (->> (segment-metadata-query host datasource)
       :aggregators
       (map druid-column-result-to-map)))

(defn send-query-with-single-result [& args]
  (-> (apply send-query args) first :result))

; TODO usar specter para transformar las keys de forma general
(defn time-boundary [host ds]
  (-> (send-query-with-single-result host {:queryType "timeBoundary" :dataSource ds})
      (clojure.set/rename-keys {:minTime :min-time :maxTime :max-time})))

(defn- assoc-if-seq [map key val]
  (cond-> map
          (seq val) (assoc key val)))

(defn- to-druid-agg [{:keys [name type] :or {type "doubleSum"}}]
  {:fieldName name :name name :type type})

; TODO repetida en el client
(defn dim=? [dim1 dim2]
  (= (:name dim1) (:name dim2)))

; TODO repetida en el client
(defn includes-dim? [coll dim]
  (some #(dim=? % dim) coll))

(defn- add-sort-by-dim-to-aggregations [{:keys [sort-by] :as dim} measures aggregations]
  "If we sort by a not selected metric we should send the field as an aggregation, otherwise Druid complains"
  (if (or (not sort-by) (includes-dim? measures {:name sort-by}))
    aggregations
    (conj aggregations (to-druid-agg {:name sort-by}))))

(defn- to-druid-filter [[{:keys [name is]} :as filters]]
  (condp = (count filters)
    0 nil
    1 {:type "selector" :dimension name :value is}
    {:type "and" :fields (map #(to-druid-filter [%]) filters)}))

; TODO estas dos estan duplicadas en el client
(defn time-dimension? [{:keys [name]}]
  (= name "__time"))

(defn- calculate-query-type [{:keys [dimension]}]
  (if (and dimension (not (time-dimension? dimension)))
    "topN"
    "timeseries"))

; TODO el threshold en las timeseries query no existe, quizas habria que limitar en mem nomas
(defn- add-query-type-dependant-fields [{:keys [dimension measures] :as q}
                                        {:keys [queryType] :as dq}]
  (condp = queryType
    "topN"
    (assoc dq
           :granularity {:type "all"}
           :dimension (dimension :name)
           :metric {:type (if (get dimension :descending true) "numeric" "inverted")
                    :metric (get dimension :sort-by (-> measures first :name))}
           :threshold (dimension :limit (or 100)))
    "timeseries"
    (assoc dq
           :granularity (if dimension
                          {:type "period" :period (:granularity dimension)}
                          {:type "all"})
           :descending (get dimension :descending false))))

; TODO creo que convendria validar esta q con clojure spec xq sino si falta el period por ej explota en druid, o sino validar la que se envia a druid directamente que ya tenemos los valores obligatorios en la doc
(defn to-druid-query [{:keys [cube measures interval dimension filter] :as q}]
  (as-> {:queryType (calculate-query-type q)
         :dataSource {:type "table" :name cube}
         :intervals (str/join "/" interval)
         :aggregations (->> measures (mapv to-druid-agg) (add-sort-by-dim-to-aggregations dimension measures))}
        dq
        (add-query-type-dependant-fields q dq)
        (assoc-if-seq dq :filter (to-druid-filter (remove time-dimension? filter)))))

(defn from-druid-results [{:keys [dimension]} {:keys [queryType]} results]
  (condp = queryType
    "topN" (-> results first :result)
    "timeseries" (map (fn [{:keys [result timestamp]}]
                        (if dimension
                          (assoc result :__time timestamp)
                          result))
                      results)))

(defn- send-query-and-simplify-results [host q]
  (let [dq (to-druid-query q)
        dr (send-query host dq)]
    (from-druid-results q dq dr)))

(defn- add-filter-for-dim [filter {:keys [name]} result]
  (let [dim-value (result (keyword name))]
    (conj filter {:name name :is dim-value})))

; TODO no me convencen los nombres de algunas de estas funciones
(defn- send-queries-for-split [host {:keys [split filter] :as q}]
  (let [[dim & dims] split]
    (when dim
      (->> (send-query-and-simplify-results host (assoc q :dimension dim))
           (pmap #(assoc-if-seq % :_results
                    (send-queries-for-split host
                      (assoc q :split dims
                               :filter (add-filter-for-dim filter dim %)))))))))

; TODO quizas convenga traer las dimensions y metrics en la misma query para ahorrar un request
(defrecord DruidEngine [host]
  DwEngine
  (cubes [_]
         (datasources host))
  (cube [_ name]
        (assoc {:name name}
               :dimensions (dimensions host name)
               :measures (metrics host name)
               :time-boundary (time-boundary host name)))
  (query [_ {:keys [totals] :as q}]
         (concat (if totals (send-query-and-simplify-results host q) [])
                 (send-queries-for-split host q))))

;;;; Manual testing
(def broker "http://kafka:8082")

#_(datasources broker)
#_(dimensions broker "vtol_stats")
#_(dimensions broker "wikiticker")
#_(metrics broker "vtol_stats")
#_(time-boundary broker "wikiticker")
#_(e/cube (DruidEngine. broker) "vtol_stats")

; Totals query
#_(e/query (DruidEngine. broker)
           {:cube "wikiticker"
            :measures [{:name "count" :type "longSum"}
                       {:name "added" :type "doubleSum"}]
            :interval ["2015-09-12" "2015-09-13"]
            :totals true})

; One no-time dimension and one measure
#_(e/query (DruidEngine. broker)
           {:cube "wikiticker"
            :split [{:name "page" :limit 5}]
            :measures [{:name "count" :type "longSum"}]
            :interval ["2015-09-12" "2015-09-13"]})

; One time dimension and one measure
#_(e/query (DruidEngine. broker)
           {:cube "wikiticker"
            :split [{:name "__time" :granularity "PT6H"}]
            :measures [{:name "count" :type "longSum"}]
            :interval ["2015-09-12" "2015-09-13"]})

; One no-time dimension with totals
#_(e/query (DruidEngine. broker)
           {:cube "wikiticker"
            :split [{:name "page" :limit 5}]
            :measures [{:name "count" :type "longSum"}]
            :interval ["2015-09-12" "2015-09-13"]
            :totals true})

; Two no-time dimensions
#_(e/query (DruidEngine. broker)
           {:cube "wikiticker"
            :split [{:name "isMinor" :limit 3} {:name "isRobot" :limit 2}]
            :measures [{:name "count" :type "longSum"}]
            :interval ["2015-09-12" "2015-09-13"]
            :totals true})

; Three no-time dimensions
#_(e/query (DruidEngine. broker)
           {:cube "wikiticker"
            :split [{:name "isMinor" :limit 3} {:name "isRobot" :limit 2} {:name "isNew" :limit 2}]
            :measures [{:name "count" :type "longSum"}]
            :interval ["2015-09-12" "2015-09-13"]
            :totals true})
