(ns shevek.engine.druid-native.raw
  (:require [shevek.schemas.query :refer [RawQuery RawQueryResults]]
            [schema.core :as s]
            [shevek.querying.expansion :refer [expand-query]]
            [shevek.driver.druid :refer [send-query]]
            [shevek.engine.druid-native.solver.common :refer [add-druid-filters]]
            [shevek.lib.collections :refer [assoc-if]]
            [clojure.set :refer [rename-keys]]))

(defn to-druid-query [{:keys [cube paging] :or {paging {:threshold 100}} :as q}]
  (-> {:queryType "select"
       :dataSource cube
       :granularity "all"
       :pagingSpec paging}
      (add-druid-filters q)))

(defn from-druid-results [{:keys [paging]} dr]
  (let [{:keys [events pagingIdentifiers]} (-> dr first :result)]
    {:results (map (comp #(rename-keys % {:timestamp :__time}) :event) events)
     :paging (assoc-if paging :pagingIdentifiers pagingIdentifiers)}))

(s/defn execute-query :- RawQueryResults [driver query :- RawQuery cube-schema]
  (->> (expand-query query cube-schema)
       (to-druid-query)
       (send-query driver)
       (from-druid-results query)))
