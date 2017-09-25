(ns shevek.querying.aggregation
  (:require [shevek.lib.collections :refer [assoc-if-seq find-by]]
            [shevek.lib.druid-driver :as driver]
            [shevek.lib.dates :refer [plus-period]]
            [shevek.lib.dw.dims :refer [time-dimension? partition-splits row-split? col-split?]]
            [shevek.lib.collections :refer [detect]]
            [shevek.querying.conversion :refer [to-druid-query from-druid-results]]
            [shevek.schemas.query :refer [Query]]
            [schema.core :as s]
            [com.rpl.specter :refer [setval ALL]]))

(defn- send-query [dw q]
  (let [dq (to-druid-query q)
        dr (driver/send-query dw dq)]
    (from-druid-results q dq dr)))

(defn- dim-value [{:keys [name]} result]
  (result (keyword name)))

(defn- add-filter-for-dim [filters {:keys [granularity] :as dim} result]
  (let [value (dim-value dim result)]
    (if (time-dimension? dim)
      (setval [ALL #(:interval %) :interval]
              [value (plus-period value granularity)] filters)
      (conj filters (assoc dim :operator "is" :value value)))))

(defn- complete-column-results [grand-total-dim-values dim results]
  (if (seq grand-total-dim-values)
    (let [corresponding-result (fn [value] (detect #(= (dim-value dim %) value) results))]
      (map corresponding-result grand-total-dim-values))
    results))

(defn- resolve-col-splits [dw {:keys [splits filters] :as q} grand-total]
  (let [[dim & dims] (filter col-split? splits)
        nested-query #(assoc q :splits %1 :filters (add-filter-for-dim filters dim %2))
        posible-values (->> grand-total (mapcat :child-cols) (map (partial dim-value dim)) distinct)
        this-q (cond-> (assoc q :dimension dim)
                       (seq grand-total) (update :filters conj (assoc dim :operator "include" :value posible-values)))]
    (when dim
      (->> (send-query dw this-q)
           (pmap #(assoc-if-seq % :child-cols (resolve-col-splits dw (nested-query dims %) (mapcat :child-cols grand-total))))
           (complete-column-results posible-values dim)
           doall))))

(defn- resolve-row-splits [dw {:keys [splits filters] :as q} grand-total]
  (let [[row-splits col-splits] (partition-splits splits)
        [dim & dims] row-splits
        nested-query #(assoc q :splits %1 :filters (add-filter-for-dim filters dim %2))
        nested-child-rows #(resolve-row-splits dw (nested-query (concat dims col-splits) %) grand-total)
        nested-child-cols #(resolve-col-splits dw (nested-query col-splits %) grand-total)]
    (when dim
      (->> (send-query dw (assoc q :dimension dim))
           (pmap #(assoc-if-seq % :child-rows (nested-child-rows %) :child-cols (nested-child-cols %)))
           doall))))

(defn- resolve-grand-totals [dw q]
  (->> (send-query dw q)
       (pmap #(assoc-if-seq % :child-cols (resolve-col-splits dw q [])))))

(s/defn query [dw {:keys [cube totals] :as q} :- Query]
  (let [grand-totals (if totals (resolve-grand-totals dw q) [])]
    (concat grand-totals
            (resolve-row-splits dw q grand-totals))))

;; Examples

; Totals query
#_(query shevek.dw/dw
         {:cube "wikiticker"
          :measures [{:name "count" :expression "(sum $count)"}
                     {:name "added" :expression "(sum $added)"}]
          :filters [{:interval ["2015-09-12" "2015-09-13"]}]
          :totals true})

; One dimension and one measure
#_(query shevek.dw/dw
         {:cube "wikiticker"
          :splits [{:name "page" :limit 5}]
          :measures [{:name "count" :expression "(sum $count)"}]
          :filters [{:interval ["2015-09-12" "2015-09-13"]}]})

; One time dimension and one measure
#_(query shevek.dw/dw
         {:cube "wikiticker"
          :splits [{:name "__time" :granularity "PT12H"}]
          :measures [{:name "count" :expression "(sum $count)"}]
          :filters [{:interval ["2015-09-12" "2015-09-13"]}]})

; One dimension with totals
#_(query shevek.dw/dw
         {:cube "wikiticker"
          :splits [{:name "page" :limit 5}]
          :measures [{:name "count" :expression "(sum $count)"}]
          :filters [{:interval ["2015-09-12" "2015-09-13"]}]
          :totals true})

; Two dimensions
#_(query shevek.dw/dw
         {:cube "wikiticker"
          :splits [{:name "countryName" :limit 3} {:name "cityName" :limit 2}]
          :measures [{:name "count" :expression "(sum $count)"}]
          :filters [{:interval ["2015-09-12" "2015-09-13"]}]
          :totals true})

; Three dimensions
#_(query shevek.dw/dw
         {:cube "wikiticker"
          :splits [{:name "isMinor" :limit 3} {:name "isRobot" :limit 2} {:name "isNew" :limit 2}]
          :measures [{:name "count" :expression "(sum $count)"}]
          :filters [{:interval ["2015-09-12" "2015-09-13"]}]
          :totals true})

; Time and normal dimension together
#_(query shevek.dw/dw
         {:cube "wikiticker"
          :splits [{:name "__time" :granularity "PT6H"} {:name "isNew"}]
          :measures [{:name "count" :expression "(sum $count)"}]
          :filters [{:interval ["2015-09-12" "2015-09-13"]}]})

; Filtering
#_(query shevek.dw/dw
         {:cube "wikiticker"
          :splits [{:name "countryName" :limit 5}]
          :filters [{:interval ["2015-09-12" "2015-09-13"]}
                    {:name "countryName" :operator "include" :value #{"Italy" "France"}}]
          :measures [{:name "count" :expression "(sum $count)"}]})

#_(query shevek.dw/dw
         {:cube "wikiticker"
          :splits [{:name "countryName" :limit 5}]
          :filters [{:interval ["2015-09-12" "2015-09-13"]}
                    {:name "countryName" :operator "search" :value "arg"}]
          :measures [{:name "count" :expression "(sum $count)"}]})

; Sorting
#_(query shevek.dw/dw
         {:cube "wikiticker"
          :splits [{:name "page" :limit 5 :sort-by {:name "added" :expression "(sum $count)" :descending false}}]
          :measures [{:name "count" :expression "(sum $count)"}]
          :filters [{:interval ["2015-09-12" "2015-09-13"]}]})

#_(query shevek.dw/dw
         {:cube "wikiticker"
          :splits [{:name "page" :limit 5 :sort-by {:name "page" :descending false}}]
          :measures [{:name "count" :expression "(sum $count)"}]
          :filters [{:interval ["2015-09-12" "2015-09-13"]}]})

; Different time zone
#_(query shevek.dw/dw
         {:cube "wikiticker"
          :splits [{:name "__time" :granularity "P1D"}]
          :measures [{:name "count" :expression "(sum $count)"}]
          :filters [{:interval ["2015-09-12" "2015-09-13"]}]
          :time-zone "Europe/Paris"})

; One column split and two measures
#_(query shevek.dw/dw
         {:cube "wikiticker"
          :splits [{:name "isUnpatrolled" :limit 2 :on "columns"}]
          :measures [{:name "count" :expression "(sum $count)"}
                     {:name "added" :expression "(sum $added)"}]
          :filters [{:interval ["2015-09-12" "2015-09-13"]}]
          :totals true})

; Two row splits, one column split and one measure
#_(query shevek.dw/dw
         {:cube "wikiticker"
          :splits [{:name "countryName" :limit 2 :on "rows"}
                   {:name "cityName" :limit 2 :on "rows"}
                   {:name "isUnpatrolled" :limit 2 :on "columns"}]
          :measures [{:name "count" :expression "(sum $count)"}]
          :filters [{:interval ["2015-09-12" "2015-09-13"]}
                    {:name "countryName" :operator "exclude" :value #{nil}}
                    {:name "cityName" :operator "exclude" :value #{nil}}]
          :totals true})

; Different child-cols values for different parents
#_(query shevek.dw/dw
         {:cube "wikiticker"
          :filters [{:name "__time" :interval ["2015" "2016"]}
                    {:name "countryName" :operator "include" :value #{"Italy" "United States" "Russia"}}]
          :splits [{:name "countryName" :on "rows"}
                   {:name "isUnpatrolled" :on "columns"}]
          :measures [{:name "count" :expression "(sum $count)"}]
          :totals true})
