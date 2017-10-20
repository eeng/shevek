(require '[shevek.querying.api2 :refer [query]])
(def request {})

; Totals query
#_(query request
         {:cube "wikiticker"
          :measures ["count" "added"]
          :filters [{:period "latest-day"}]})

; One dimension and one measure
#_(query request
         {:cube "wikiticker"
          :splits [{:name "page" :limit 5}]
          :measures ["count"]
          :filters [{:period "latest-day"}]})

; One time dimension and one measure
#_(query request
         {:cube "wikiticker"
          :splits [{:name "__time" :granularity "PT12H"}]
          :measures ["count"]
          :filters [{:period "latest-day"}]})

; One dimension with totals
#_(query request
         {:cube "wikiticker"
          :splits [{:name "page" :limit 5}]
          :measures ["count"]
          :filters [{:period "latest-day"}]
          :totals true})

; Two dimensions
#_(query request
         {:cube "wikiticker"
          :splits [{:name "countryName" :limit 3} {:name "cityName" :limit 2}]
          :measures ["count"]
          :filters [{:period "latest-day"}]
          :totals true})

; Three dimensions
#_(query request
         {:cube "wikiticker"
          :splits [{:name "isMinor" :limit 3} {:name "isRobot" :limit 2} {:name "isNew" :limit 2}]
          :measures ["count"]
          :filters [{:period "latest-day"}]
          :totals true})

; Time and normal dimension together
#_(query request
         {:cube "wikiticker"
          :splits [{:name "__time" :granularity "PT6H"} {:name "isNew"}]
          :measures ["count"]
          :filters [{:period "latest-day"}]})

; Filtering
#_(query request
         {:cube "wikiticker"
          :splits [{:name "countryName" :limit 5}]
          :filters [{:interval ["2015" "2016"]}
                    {:name "countryName" :operator "include" :value #{"Italy" "France"}}]
          :measures ["count"]})

#_(query request
         {:cube "wikiticker"
          :splits [{:name "countryName" :limit 5}]
          :filters [{:period "latest-day"}
                    {:name "countryName" :operator "search" :value "arg"}]
          :measures ["count"]})

; Sorting
#_(query request
         {:cube "wikiticker"
          :splits [{:name "page" :limit 5 :sort-by {:name "added" :descending false}}]
          :measures ["count"]
          :filters [{:period "latest-day"}]})

#_(query request
         {:cube "wikiticker"
          :splits [{:name "page" :limit 5 :sort-by {:name "page" :descending false}}]
          :measures ["count"]
          :filters [{:period "latest-day"}]})

; Different time zone
#_(query request
         {:cube "wikiticker"
          :splits [{:name "__time" :granularity "P1D"}]
          :measures ["count"]
          :filters [{:period "latest-day"}]
          :time-zone "Europe/Paris"})

; One column split and two measures
#_(query request
         {:cube "wikiticker"
          :splits [{:name "isUnpatrolled" :limit 2 :on "columns"}]
          :measures ["count" "added"]
          :filters [{:period "latest-day"}]
          :totals true})

; Two row splits, one column split and one measure
#_(query request
         {:cube "wikiticker"
          :splits [{:name "countryName" :limit 2 :on "rows"}
                   {:name "cityName" :limit 2 :on "rows"}
                   {:name "isUnpatrolled" :limit 2 :on "columns"}]
          :measures ["count"]
          :filters [{:period "latest-day"}
                    {:name "countryName" :operator "exclude" :value #{nil}}
                    {:name "cityName" :operator "exclude" :value #{nil}}]
          :totals true})

; Different child-cols values for different parents
#_(query request
         {:cube "wikiticker"
          :filters [{:name "__time" :interval ["2015" "2016"]}
                    {:name "countryName" :operator "include" :value #{"Italy" "United States" "Russia"}}]
          :splits [{:name "countryName" :on "rows"}
                   {:name "isUnpatrolled" :on "columns"}]
          :measures ["count"]
          :totals true})

; Extraction functions
#_(query request
         {:cube "wikiticker"
          :filters [{:period "latest-day"}
                    {:name "año" :operator "include" :value #{"2015"} :column "__time" :extraction [{:type "timeFormat" :format "Y" :locale "es"}]}]
          :measures ["count"]
          :totals true})
