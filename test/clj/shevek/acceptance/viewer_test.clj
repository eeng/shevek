(ns shevek.acceptance.viewer-test
  (:require [clojure.test :refer :all]
            [shevek.acceptance.test-helper :refer :all]
            [etaoin.api :refer :all]
            [shevek.support.druid :refer :all]
            [shevek.makers :refer [make!]]
            [shevek.schemas.cube :refer [Cube]]))

(defn make-wikiticker-cube []
  (make! Cube
    {:name "wikiticker",
     :dimensions [{:name "regionName",
                   :type "STRING",
                   :title "Region Name"}
                  {:name "regionIsoCode",
                   :type "STRING",
                   :title "Region Iso Code"}
                  {:name "isMinor", :type "STRING", :title "Is Minor"}
                  {:name "isUnpatrolled",
                   :type "STRING",
                   :title "Is Unpatrolled"}
                  {:name "channel", :type "STRING", :title "Channel"}
                  {:name "page", :type "STRING", :title "Page"}
                  {:name "countryName",
                   :type "STRING",
                   :title "Country Name"}
                  {:name "__time", :type "LONG", :title "Time"}
                  {:name "isAnonymous",
                   :type "STRING",
                   :title "Is Anonymous"}
                  {:name "isNew", :type "STRING", :title "Is New"}
                  {:name "metroCode", :type "STRING", :title "Metro Code"}
                  {:name "countryIsoCode",
                   :type "STRING",
                   :title "Country Iso Code"}
                  {:name "comment", :type "STRING", :title "Comment"}
                  {:name "cityName", :type "STRING", :title "City Name"}
                  {:name "namespace", :type "STRING", :title "Namespace"}
                  {:name "user", :type "STRING", :title "User"}
                  {:name "isRobot", :type "STRING", :title "Is Robot"}],
     :title "Wikiticker",
     :measures [{:expression "(sum $deleted)",
                 :name "deleted",
                 :type "longSum",
                 :title "Deleted"}
                {:expression "(sum $added)",
                 :name "added",
                 :type "longSum",
                 :title "Added"}
                {:expression "(sum $count)",
                 :name "count",
                 :type "longSum",
                 :title "Count"}
                {:expression "(sum $delta)",
                 :name "delta",
                 :type "longSum",
                 :title "Delta"}
                {:expression "(count-distinct $user_unique)",
                 :name "user_unique",
                 :type "hyperUnique",
                 :title "User Unique"}]}))

(defn go-to-viewer [page]
  (make-wikiticker-cube)
  (visit page "/")
  (click page {:css "#cubes-menu"})
  (click-link page "Wikiticker"))

(deftest viewer
  (it "shows dimensions, measures and basic stats on page entry" page
    (with-fake-druid
      {(query-req-matching #"queryType.*timeBoundary") (druid-res "acceptance/time-boundary")
       (query-req-matching #"queryType.*timeseries") (druid-res "acceptance/totals")}
      (go-to-viewer page)
      (is (has-css? page ".statistic" :count 3))
      (is (has-css? page ".statistics" :text "394298"))
      (is (has-css? page ".dimensions" :text "Region Name"))
      (is (has-css? page ".measures" :text "User Unique"))))

  (it "should restore viewer after refresh" page
    (with-fake-druid
      {(query-req-matching #"queryType.*timeBoundary") (druid-res "acceptance/time-boundary")
       (query-req-matching #"queryType.*timeseries") (druid-res "acceptance/totals")}
      (go-to-viewer page)
      (is (has-css? page ".statistic" :count 3))
      (click page {:id "cb-measure-delta"})
      (is (has-css? page ".statistic" :count 4))
      (Thread/sleep 300) ; For the debounce
      (refresh page)
      (is (has-css? page ".statistic" :count 4)))))
