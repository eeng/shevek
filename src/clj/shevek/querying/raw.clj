(ns shevek.querying.raw
  (:require [shevek.schemas.query :refer [RawQuery RawQueryResults]]
            [schema.core :as s]
            [shevek.querying.conversion :refer [add-druid-filters]]
            [shevek.lib.druid-driver :refer [send-query]]
            [shevek.schema.repository :refer [find-cube]]
            [clojure.set :refer [rename-keys]]))

; fromNext should not be necessary on the next version of Druid
(defn to-druid-query [{:keys [cube filter paging] :or {paging {:threshold 100}}} schema]
  (-> {:queryType "select"
       :dataSource {:type "table" :name cube}
       :granularity {:type "all"}
       :pagingSpec (assoc paging :fromNext true)}
      (add-druid-filters filter schema)))

(defn from-druid-results [{:keys [paging]} dr]
  (let [{:keys [events pagingIdentifiers]} (-> dr first :result)]
    {:results (map (comp #(rename-keys % {:timestamp :__time}) :event) events)
     :paging (assoc paging :pagingIdentifiers pagingIdentifiers)}))

(s/defn query :- RawQueryResults [dw db {:keys [cube] :as q} :- RawQuery]
  (let [schema (find-cube db cube)
        dq (to-druid-query q schema)
        dr (send-query dw dq)]
    (from-druid-results q dr)))

;; Examples

#_(query shevek.dw/dw shevek.db/db
         {:cube "wikiticker"
          :filter [{:interval ["2015" "2016"]}]
          :paging {:threshold 3}})

; Getting the second page
#_(let [result (query shevek.dw/dw shevek.db/db
                      {:cube "wikiticker"
                       :filter [{:interval ["2015" "2016"]}]
                       :paging {:threshold 3}})]
    (query shevek.dw/dw shevek.db/db
           {:cube "wikiticker"
            :filter [{:interval ["2015" "2016"]}]
            :paging (:paging result)}))
