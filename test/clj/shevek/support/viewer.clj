(ns shevek.support.viewer
  (:require [shevek.acceptance.test-helper :refer :all]
            [etaoin.api :refer :all]
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
  (login page)
  (click page {:css "#cubes-menu"})
  (click-link page "Wikiticker"))
