(ns pivot.druid
  (:require [clj-http.client :as http]))

(def broker "http://kafka:8082/druid/v2")

; TODO http-kit viene con un client, ver si no se puede usar para no tener q agregar otra dependencia
(defn datasources [host]
  (:body (http/get (str host "/datasources") {:as :json})))

; TODO en el :context de la q se le puede pasar un timeout
(defn- query [host q]
  (:body (http/post (str host "/") {:content-type :json :form-params q :as :json})))

; TODO candidata a memoizar no?
(defn- segment-metadata-query [host datasource]
  (let [q {:queryType "segmentMetadata"
           :dataSource {:type "table" :name datasource}
           :merge true
           :analysisTypes ["cardinality" "aggregators"]
           :intervals ["2000/2100"]
           :lenientAggregatorMerge true}]
    (first (query host q))))

(defn- druid-column-result-to-map [[column fields]]
  (merge (select-keys fields [:type :cardinality])
         {:name (name column)}))

(defn dimensions [host datasource]
  (->> (segment-metadata-query host datasource)
       :columns
       (map druid-column-result-to-map)))

(defn measures [host datasource]
  (->> (segment-metadata-query host datasource)
       :aggregators
       (map druid-column-result-to-map)))

#_(datasources broker)
#_(dimensions broker "vtol_stats")
#_(dimensions broker "wikiticker")
#_(measures broker "vtol_stats")
