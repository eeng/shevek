(ns shevek.querying.aggregation
  (:require [shevek.lib.collections :refer [assoc-if-seq]]
            [shevek.lib.druid-driver :refer [send-query]]
            [shevek.lib.dates :refer [plus-period]]
            [shevek.lib.dw.dims :refer [time-dimension?]]
            [shevek.querying.conversion :refer [to-druid-query from-druid-results]]
            [shevek.schemas.query :refer [Query]]
            [schema.core :as s]
            [com.rpl.specter :refer [setval ALL]]))

(defn- send-query-and-simplify-results [dw q]
  (let [dq (to-druid-query q)
        dr (send-query dw dq)]
    (from-druid-results q dq dr)))

(defn- add-filter-for-dim [filter {:keys [name granularity] :as dim} result]
  (let [dim-value (result (keyword name))]
    (if (time-dimension? dim)
      (setval [ALL :interval] [dim-value (plus-period dim-value granularity)] filter)
      (conj filter {:name name :operator "is" :value dim-value}))))

(defn- send-queries-for-split [dw {:keys [split filter] :as q}]
  (let [[dim & dims] split]
    (when dim
      (->> (send-query-and-simplify-results dw (assoc q :dimension dim))
           (pmap #(assoc-if-seq % :_results
                    (send-queries-for-split dw
                      (assoc q :split dims
                               :filter (add-filter-for-dim filter dim %)))))
           doall))))

(s/defn query [dw {:keys [totals] :as q} :- Query]
  (concat (if totals (send-query-and-simplify-results dw q) [])
          (send-queries-for-split dw q)))

;; Examples

; Totals query
#_(query shevek.dw/dw
         {:cube "wikiticker"
          :measures [{:name "count" :expression "(sum $count)"}
                     {:name "added" :expression "(sum $added)"}]
          :filter [{:interval ["2015-09-12" "2015-09-13"]}]
          :totals true})

; One dimension and one measure
#_(query shevek.dw/dw
         {:cube "wikiticker"
          :split [{:name "page" :limit 5}]
          :measures [{:name "count" :expression "(sum $count)"}]
          :filter [{:interval ["2015-09-12" "2015-09-13"]}]})

; One time dimension and one measure
#_(query shevek.dw/dw
         {:cube "wikiticker"
          :split [{:name "__time" :granularity "PT12H"}]
          :measures [{:name "count" :expression "(sum $count)"}]
          :filter [{:interval ["2015-09-12" "2015-09-13"]}]})

; One dimension with)
#_(query shevek.dw/dw
         {:cube "wikiticker"
          :split [{:name "page" :limit 5}]
          :measures [{:name "count" :expression "(sum $count)"}]
          :filter [{:interval ["2015-09-12" "2015-09-13"]}]
          :totals true})

; Two dimensions
#_(query shevek.dw/dw
         {:cube "wikiticker"
          :split [{:name "countryName" :limit 3} {:name "cityName" :limit 2}]
          :measures [{:name "count" :expression "(sum $count)"}]
          :filter [{:interval ["2015-09-12" "2015-09-13"]}]
          :totals true})

; Three dimensions
#_(query shevek.dw/dw
         {:cube "wikiticker"
          :split [{:name "isMinor" :limit 3} {:name "isRobot" :limit 2} {:name "isNew" :limit 2}]
          :measures [{:name "count" :expression "(sum $count)"}]
          :filter [{:interval ["2015-09-12" "2015-09-13"]}]
          :totals true})

; Time and normal dimension together
#_(query shevek.dw/dw
         {:cube "wikiticker"
          :split [{:name "__time" :granularity "PT6H"} {:name "isNew"}]
          :measures [{:name "count" :expression "(sum $count)"}]
          :filter [{:interval ["2015-09-12" "2015-09-13"]}]})

; Filtering
#_(query shevek.dw/dw
         {:cube "wikiticker"
          :split [{:name "countryName" :limit 5}]
          :filter [{:interval ["2015-09-12" "2015-09-13"]}
                   {:name "countryName" :operator "include" :value #{"Italy" "France"}}]
          :measures [{:name "count" :expression "(sum $count)"}]})

#_(query shevek.dw/dw
         {:cube "wikiticker"
          :split [{:name "countryName" :limit 5}]
          :filter [{:interval ["2015-09-12" "2015-09-13"]}
                   {:name "countryName" :operator "search" :value "arg"}]
          :measures [{:name "count" :expression "(sum $count)"}]})

; Sorting
#_(query shevek.dw/dw
         {:cube "wikiticker"
          :split [{:name "page" :limit 5 :sort-by {:name "added" :expression "(sum $count)" :descending false}}]
          :measures [{:name "count" :expression "(sum $count)"}]
          :filter [{:interval ["2015-09-12" "2015-09-13"]}]})

#_(query shevek.dw/dw
         {:cube "wikiticker"
          :split [{:name "page" :limit 5 :sort-by {:name "page" :descending false}}]
          :measures [{:name "count" :expression "(sum $count)"}]
          :filter [{:interval ["2015-09-12" "2015-09-13"]}]})
