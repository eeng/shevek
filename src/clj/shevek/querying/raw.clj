(ns shevek.querying.raw
  (:require [shevek.schemas.query :refer [RawQuery RawQueryResults]]
            [schema.core :as s]
            [shevek.querying.conversion :refer [add-druid-filters]]
            [shevek.lib.druid-driver :refer [send-query]]
            [shevek.lib.collections :refer [assoc-if]]
            [clojure.set :refer [rename-keys]]))

; fromNext should not be necessary on the next version of Druid
(defn to-druid-query [{:keys [cube paging] :or {paging {:threshold 100}} :as q}]
  (-> {:queryType "select"
       :dataSource {:type "table" :name cube}
       :granularity {:type "all"}
       :pagingSpec (assoc paging :fromNext true)}
      (add-druid-filters q)))

(defn from-druid-results [{:keys [paging]} dr]
  (let [{:keys [events pagingIdentifiers]} (-> dr first :result)]
    {:results (map (comp #(rename-keys % {:timestamp :__time}) :event) events)
     :paging (assoc-if paging :pagingIdentifiers pagingIdentifiers)}))

(s/defn query :- RawQueryResults [dw {:keys [cube] :as q} :- RawQuery]
  (let [dq (to-druid-query q)
        dr (send-query dw dq)]
    (from-druid-results q dr)))
