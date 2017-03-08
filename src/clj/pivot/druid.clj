(ns pivot.druid
  (:require [clj-http.client :as http]))

(def broker "http://kafka:8082")

; TODO http-kit viene con un client, ver si no se puede usar para no tener q agregar otra dependencia
(defn datasources [host]
  (->> (http/get (str host "/druid/v2/datasources") {:as :json :conn-timeout 10000})
       :body (map (fn [name] {:name name}))))

; TODO en el :context de la q se le puede pasar un timeout
(defn- query [host q]
  (:body (http/post (str host "/druid/v2") {:content-type :json :form-params q :as :json})))

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

(defn metrics [host datasource]
  (->> (segment-metadata-query host datasource)
       :aggregators
       (map druid-column-result-to-map)))

#_(datasources broker)
#_(dimensions broker "vtol_stats")
#_(dimensions broker "wikiticker")
#_(metrics broker "vtol_stats")
