(ns pivot.druid
  (:require [clj-http.client :as http]))

(def broker "http://kafka:8082/druid/v2")

(defn datasources [host]
  (:body (http/get (str host "/datasources") {:as :json})))

; TODO en el :context de la q se le puede pasar un timeout
(defn- query [host q]
  (:body (http/post (str host "/") {:content-type :json :form-params q :as :json})))

(defn- druid-column-result-to-map [[column fields]]
  (merge (select-keys fields [:type :cardinality])
         {:name (name column)}))

(defn dimensions [host datasource]
  (let [q {:queryType "segmentMetadata"
           :dataSource {:type "table" :name datasource}
           :merge true}]
    (->> (query host q) first :columns (map druid-column-result-to-map))))

(defn measures [host datasource]
  (let [q {:queryType "segmentMetadata"
           :dataSource {:type "table" :name datasource}
           :merge true
           :analysisTypes ["aggregators"]}]
    (->> (query host q) first :aggregators (map druid-column-result-to-map))))

#_(datasources broker)
#_(dimensions broker "vtol_stats")
#_(dimensions broker "wikiticker")
#_(measures broker "vtol_stats")
