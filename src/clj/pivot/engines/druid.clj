(ns pivot.engines.druid
  (:require [clj-http.client :as http]
            [pivot.engines.engine :as e :refer [DwEngine]]
            [clojure.string :as str]))

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

(defn to-druid-query [{:keys [cube measures interval]}]
  {:queryType "timeseries"
   :dataSource {:type "table" :name cube}
   :granularity {:type "all"}
   :intervals (str/join "/" interval)
   :aggregations (map to-druid-agg measures)})

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
           (clojure.pprint/pprint dq) ; TODO pasar a logging
           (send-query host dq))))

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

; One dimension and one measure query
#_(query broker {:cube "wikiticker"
                 :measures [{:name "count" :type "longSum"}
                            {:name "added" :type "doubleSum"}]
                 :interval ["2015-09-12" "2015-09-13"]})
