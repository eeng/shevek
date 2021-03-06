(require '[shevek.querying.api :refer [query raw-query]] :reload)

(def request {})
(comment
  ; Totals query
  (query request
         {:cube "wikipedia"
          :measures ["added" "deleted"]
          :filters [{:period "latest-day"}]})

  ; One dimension and one measure
  (query request
         {:cube "wikipedia"
          :splits [{:name "page" :limit 5}]
          :measures ["added"]
          :filters [{:period "latest-day"}]})

  ; One time dimension and one measure
  (query request
         {:cube "wikipedia"
          :splits [{:name "__time" :granularity "PT12H"}]
          :measures ["added"]
          :filters [{:period "latest-day"}]})

  ; One dimension with totals
  (query request
         {:cube "wikipedia"
          :splits [{:name "page" :limit 5}]
          :measures ["added"]
          :filters [{:period "latest-day"}]
          :totals true})

  ; Two dimensions
  (query request
         {:cube "wikipedia"
          :splits [{:name "countryName" :limit 3} {:name "cityName" :limit 2}]
          :measures ["added"]
          :filters [{:period "latest-day"}]
          :totals true})

  ; Three dimensions
  (query request
         {:cube "wikipedia"
          :splits [{:name "isMinor" :limit 3} {:name "isRobot" :limit 2} {:name "isNew" :limit 2}]
          :measures ["added"]
          :filters [{:period "latest-day"}]
          :totals true})

  ; Time and normal dimension together
  (query request
         {:cube "wikipedia"
          :splits [{:name "__time" :granularity "PT6H"} {:name "isNew"}]
          :measures ["added"]
          :filters [{:period "latest-day"}]})

  ; One column split and two measures
  (query request
         {:cube "wikipedia"
          :splits [{:name "isUnpatrolled" :limit 2 :on "columns"}]
          :measures ["added" "deleted"]
          :filters [{:period "latest-day"}]
          :totals true})

  ; One row split, one column split and one measure
  (query request
         {:cube "wikipedia"
          :splits [{:name "countryName" :limit 2 :on "rows"}
                   {:name "isUnpatrolled" :limit 2 :on "columns"}]
          :measures ["added"]
          :filters [{:period "latest-day"}
                    {:name "countryName" :operator "exclude" :value [nil]}]
          :totals true})

  ; Different child-cols values for different parents
  (query request
         {:cube "wikipedia"
          :filters [{:name "__time" :interval ["2015" "2016"]}
                    {:name "countryName" :operator "include" :value ["Italy" "United States" "Russia"]}]
          :splits [{:name "countryName" :on "rows"}
                   {:name "isUnpatrolled" :on "columns"}]
          :measures ["added"]
          :totals true})

  ; One row split, two column splits, and one measure
  (query request
         {:cube "wikipedia"
          :splits [{:name "countryName" :limit 2 :on "rows"}
                   {:name "isUnpatrolled" :limit 2 :on "columns"}
                   {:name "isNew" :limit 2 :on "columns"}]
          :measures ["added"]
          :filters [{:period "latest-day"}
                    {:name "countryName" :operator "exclude" :value [nil]}]
          :totals true})

  ; Two row splits, one column split, and one measure
  (query request
         {:cube "wikipedia"
          :splits [{:name "countryName" :limit 2 :on "rows"}
                   {:name "isUnpatrolled" :limit 2 :on "rows"}
                   {:name "isNew" :limit 2 :on "columns"}]
          :measures ["added"]
          :filters [{:period "latest-day"}
                    {:name "countryName" :operator "exclude" :value [nil]}]
          :totals true})

  ; Filtering
  (query request
         {:cube "wikipedia"
          :splits [{:name "countryName" :limit 5}]
          :filters [{:interval ["2015" "2016"]}
                    {:name "countryName" :operator "include" :value ["Italy" "France" 2]}]
          :measures ["added"]})

  (query request
         {:cube "wikipedia"
          :splits [{:name "countryName" :limit 5}]
          :filters [{:period "latest-day"}
                    {:name "countryName" :operator "search" :value "arg"}]
          :measures ["added"]})

  ; Sorting by measure
  (query request
         {:cube "wikipedia"
          :splits [{:name "page" :limit 5 :sort-by {:name "added" :descending false}}]
          :measures ["added"]
          :filters [{:period "latest-day"}]})

  ; Sorting by the same dimension
  (query request
         {:cube "wikipedia"
          :splits [{:name "page" :limit 5 :sort-by {:name "page" :descending false}}]
          :measures ["added"]
          :filters [{:period "latest-day"}]})

  ; Sorting by another dimension
  (query request
         {:cube "wikipedia"
          :splits [{:name "countryName" :limit 10 :sort-by {:name "countryIsoCode" :descending false}}]
          :measures ["added"]
          :filters [{:period "latest-day"}]})

  ; Sorting by another virtual dimension
  (query request
         {:cube "wikipedia"
          :splits [{:name "month" :limit 10 :sort-by {:name "monthNum" :descending false}}]
          :measures ["added"]
          :filters [{:period "latest-day"}]})

  ; Expressions
  (query request
         {:cube "wikipedia"
          :splits [{:name "monthNum2"}]
          :measures ["added"]
          :filters [{:period "latest-day"}]})

  ; Different time zone
  (query request
         {:cube "wikipedia"
          :splits [{:name "__time" :granularity "P1D"}]
          :measures ["added"]
          :filters [{:period "latest-day"}]
          :time-zone "Europe/Paris"})

  ; Extraction functions
  (query request
         {:cube "wikipedia"
          :filters [{:period "latest-day"}
                    {:name "year" :operator "include" :value ["2015"]}]
          :measures ["added"]
          :totals true})

  ; Raw query
  (raw-query request
             {:cube "wikipedia"
              :filters [{:period "latest-day"}]
              :paging {:threshold 2}})

  ; Getting the second page
  (let [result (raw-query request
                          {:cube "wikipedia"
                           :filters [{:interval ["2015" "2016"]}]
                           :paging {:threshold 3}})]
    (raw-query request
               {:cube "wikipedia"
                :filters [{:interval ["2015" "2016"]}]
                :paging (:paging result)})))
