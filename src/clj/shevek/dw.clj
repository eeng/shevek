(ns shevek.dw
  (:require [shevek.engines.engine :as e]
            [shevek.engines.memory]
            [shevek.engines.druid]
            [shevek.schemas.query :refer [Query]]
            [schema.core :as s])
  (:import [shevek.engines.memory InMemoryEngine]
           [shevek.engines.druid DruidEngine]))

#_(def engine (InMemoryEngine.))
(def engine (DruidEngine. "http://kafka:8082"))

(defn cubes []
  (e/cubes engine))

(defn cube [name]
  (e/cube engine name))

(s/defn query [q :- Query]
  (e/query engine q))

(defn max-time [q]
  (e/max-time engine q))

;; Query Examples

; Metadata
#_(cubes)
#_(cube "vtol_stats")

; Totals query
#_(query {:cube "wikiticker"
          :measures [{:name "count" :type "longSum"}
                     {:name "added" :type "doubleSum"}]
          :filter [{:interval ["2015-09-12" "2015-09-13"]}]
          :totals true})

; One dimension and one measure
#_(query {:cube "wikiticker"
          :split [{:name "page" :limit 5}]
          :measures [{:name "count" :type "longSum"}]
          :filter [{:interval ["2015-09-12" "2015-09-13"]}]})

; One time dimension and one measure
#_(query {:cube "wikiticker"
          :split [{:name "__time" :granularity "PT6H"}]
          :measures [{:name "count" :type "longSum"}]
          :filter [{:interval ["2015-09-12" "2015-09-13"]}]})

; One dimension with totals
#_(query {:cube "wikiticker"
          :split [{:name "page" :limit 5}]
          :measures [{:name "count" :type "longSum"}]
          :filter [{:interval ["2015-09-12" "2015-09-13"]}]
          :totals true})

; Two dimensions
#_(query {:cube "wikiticker"
          :split [{:name "countryName" :limit 3} {:name "cityName" :limit 2}]
          :measures [{:name "count" :type "longSum"}]
          :filter [{:interval ["2015-09-12" "2015-09-13"]}]
          :totals true})

; Three dimensions
#_(query {:cube "wikiticker"
          :split [{:name "isMinor" :limit 3} {:name "isRobot" :limit 2} {:name "isNew" :limit 2}]
          :measures [{:name "count" :type "longSum"}]
          :filter [{:interval ["2015-09-12" "2015-09-13"]}]
          :totals true})

; Filtering
#_(query {:cube "wikiticker"
          :split [{:name "countryName" :limit 5}]
          :filter [{:interval ["2015-09-12" "2015-09-13"]}
                   {:name "countryName" :operator "include" :value #{"Italy" "France"}}]
          :measures [{:name "count" :type "longSum"}]})
#_(query {:cube "wikiticker"
          :split [{:name "countryName" :limit 5}]
          :filter [{:interval ["2015-09-12" "2015-09-13"]}
                   {:name "countryName" :operator "search" :value "arg"}]
          :measures [{:name "count" :type "longSum"}]})

; Sorting
#_(query {:cube "wikiticker"
          :split [{:name "page" :limit 5 :sort-by {:name "added" :type "longSum" :descending false}}]
          :measures [{:name "count" :type "longSum"}]
          :filter [{:interval ["2015-09-12" "2015-09-13"]}]})
#_(query {:cube "wikiticker"
          :split [{:name "page" :limit 5 :sort-by {:name "page" :type "STRING" :descending false}}]
          :measures [{:name "count" :type "longSum"}]
          :filter [{:interval ["2015-09-12" "2015-09-13"]}]})
