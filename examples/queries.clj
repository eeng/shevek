(require '[shevek.querying.api :refer [querz]])

#_(querz {}
         {:cube "wikiticker"
          :measures ["count" "added"]
          :filters [{:interval ["2015" "2016"]}]
          :totals true})
