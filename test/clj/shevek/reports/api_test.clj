(ns shevek.reports.api-test
  (:require [clojure.test :refer [is]]
            [shevek.test-helper :refer [spec]]
            [shevek.makers :refer [make make!]]
            [shevek.schemas.viewer :refer [Viewer]]
            [shevek.schemas.cube :refer [Cube]]
            [shevek.reports.api :refer [save-report]]))

(spec "save-report should convert the viewer, persist it and return the report"
  (let [viewer (make Viewer {:cube (make! Cube)
                             :split [{:name "page" :type "STRING" :title "Page" :limit 10
                                      :sort-by {:name "page" :type "STRING" :title "Page" :descending true}}]})
        r (save-report {:name "Sales per Brand"} viewer)]
    (is (= "Sales per Brand" (:name r)))
    (is (= (-> viewer :cube :name)) (:cube r))
    (is (= [{:name "page" :sort-by {:name "page" :descending true}}]) (:split r))))
