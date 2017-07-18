(ns shevek.querying.raw
  (:require [shevek.schemas.query :refer [RawQuery]]
            [schema.core :as s]
            [shevek.querying.conversion :refer [add-druid-filters]]
            [shevek.lib.druid-driver :refer [send-query]]))

; fromNext should not be necessary on the next version of Druid
(defn to-druid-query [{:keys [cube filter paging] :or {paging {:threshold 100}}}]
  (-> {:queryType "select"
       :dataSource {:type "table" :name cube}
       :granularity {:type "all"}
       :pagingSpec (assoc paging :fromNext true)}
      (add-druid-filters filter)))

(defn from-druid-results [{:keys [paging]} dr]
  (let [{:keys [events pagingIdentifiers]} (-> dr first :result)]
    {:results (map :event events)
     :paging (assoc paging :pagingIdentifiers pagingIdentifiers)}))

(s/defn query [dw q :- RawQuery]
  (let [dq (to-druid-query q)
        dr (send-query dw dq)]
    (from-druid-results q dr)))

;; Examples

#_(query shevek.dw/dw
         {:cube "wikiticker"
          :filter [{:interval ["2015" "2016"]}]
          :paging {:threshold 3}})

; Getting the second page
#_(let [result (query shevek.dw/dw
                      {:cube "wikiticker"
                       :filter [{:interval ["2015" "2016"]}]
                       :paging {:threshold 3}})]
    (query shevek.dw/dw
           {:cube "wikiticker"
            :filter [{:interval ["2015" "2016"]}]
            :paging (:paging result)}))
