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
(defn- send-query [host q]
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

(defn- to-druid-agg [{:keys [name type] :or {type "doubleSum"}}]
  {:fieldName name :name name :type type})

; TODO estas dos estan duplicadas en el client
(defn time-dimension? [{:keys [name]}]
  (= name "__time"))

(defn time-dimension [dimensions]
  (detect #(time-dimension? %) dimensions))

(defn- calculate-query-type [{:keys [split]}]
  (if (and (seq split) (not (time-dimension split)))
    "topN"
    "timeseries"))

(defn- add-query-type-dependant-fields [{:keys [split measures] :as q}
                                        {:keys [queryType] :as dq}]
  (condp = queryType
    "topN"
    (assoc dq
           :granularity {:type "all"}
           :dimension (-> split first :name)
           :metric (-> measures first :name)
           :threshold (-> split first :limit (or 100)))
    "timeseries"
    (assoc dq
           :granularity (if (time-dimension split)
                          {:type "period" :period (:granularity (time-dimension split))}
                          {:type "all"}))))

; TODO creo que convendria validar esta q con clojure spec xq sino si falta el period por ej explota en druid, o sino validar la que se envia a druid directamente que ya tenemos los valores obligatorios en la doc
(defn to-druid-query [{:keys [cube measures interval split] :as q}]
  (->> {:queryType (calculate-query-type q)
        :dataSource {:type "table" :name cube}
        :intervals (str/join "/" interval)
        :aggregations (mapv to-druid-agg measures)
        :descending (or (:descending (time-dimension split)) false)}
       (add-query-type-dependant-fields q)))

(defn from-druid-results [{:keys [queryType]} results]
  (condp = queryType
    "topN" (-> results first :result)
    "timeseries" (map (fn [{:keys [result timestamp]}]
                        (assoc result :__time timestamp))
                      results)))

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
  (query [_ q]
         (let [dq (to-druid-query q)]
           (log/debug "Sending query to druid:\n" (pp-str dq))
           (from-druid-results dq (send-query host dq)))))

; Manual testing
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
            :interval ["2015-09-12" "2015-09-13"]})

; One no-time dimension and one measure (for pinned dimensions)
#_(e/query (DruidEngine. broker)
           {:cube "wikiticker"
            :split [{:name "page" :limit 5}]
            :measures [{:name "count" :type "longSum"}]
            :interval ["2015-09-12" "2015-09-13"]})

; One time dimension and one measure (for pinned time dimension)
#_(e/query (DruidEngine. broker)
           {:cube "wikiticker"
            :split [{:name "__time" :granularity "PT6H"}]
            :measures [{:name "count" :type "longSum"}]
            :interval ["2015-09-12" "2015-09-13"]})
