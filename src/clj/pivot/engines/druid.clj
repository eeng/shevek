(ns pivot.engines.druid
  (:require [clj-http.client :as http]
            [pivot.engines.engine :refer [DwEngine]]))

; TODO http-kit viene con un client, ver si no se puede usar para no tener q agregar otra dependencia
(defn datasources [host]
  (->> (http/get (str host "/druid/v2/datasources") {:as :json :conn-timeout 10000})
       :body (map (fn [name] {:name name}))))

; TODO en el :context de la q se le puede pasar un timeout
(defn- raw-query [host q]
  (:body (http/post (str host "/druid/v2") {:content-type :json :form-params q :as :json})))

; TODO candidata a memoizar no?
(defn- segment-metadata-query [host datasource]
  (let [q {:queryType "segmentMetadata"
           :dataSource {:type "table" :name datasource}
           :merge true
           :analysisTypes ["cardinality" "aggregators"]
           :intervals ["2000/2100"]
           :lenientAggregatorMerge true}]
    (first (raw-query host q))))

(defn- druid-column-result-to-map [[column fields]]
  (merge (select-keys fields [:type :cardinality])
         {:name (name column)}))

(defn dimensions [host datasource]
  (->> (segment-metadata-query host datasource)
       :columns
       (map druid-column-result-to-map)))

(defn metrics [host datasource]
  (->> (segment-metadata-query host datasource)
       :aggregators
       (map druid-column-result-to-map)))

(defn time-boundary [host ds]
  (-> (raw-query host {:queryType "timeBoundary" :dataSource ds})
      first :result))

(defn- with-dimensions-and-measures [host {:keys [name] :as datasource}]
  (assoc datasource
         :dimensions (dimensions host name)
         :measures (metrics host name)))

(defrecord DruidEngine [broker-url]
  DwEngine
  (cubes [_] (map (partial with-dimensions-and-measures broker-url)
               (datasources broker-url))))

(def broker "http://kafka:8082")
#_(datasources broker)
#_(dimensions broker "vtol_stats")
#_(dimensions broker "wikiticker")
#_(metrics broker "vtol_stats")
#_(time-boundary broker "wikiticker")

(defn totals [host ds])
#_(raw-query broker
             {:queryType "timeseries"
              :dataSource {:type "table" :name "wikiticker"}
              :granularity {:type "all"}
              :intervals ["2015-09-12T00:00:00.000Z/2015-09-13T00:00:00.000Z"]
              :aggregations [{:fieldName "count" :name "sum_count" :type "longSum"}
                             {:fieldName "added" :name "sum_added" :type "longSum"}]})
