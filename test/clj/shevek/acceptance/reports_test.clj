(ns shevek.acceptance.reports-test
  (:require [clojure.test :refer :all]
            [shevek.acceptance.test-helper :refer :all]
            [shevek.support.druid :refer [with-fake-druid query-req-matching druid-res]]
            [shevek.support.viewer :refer [make-wikiticker-cube go-to-viewer]]))

(deftest reports
  (it "creating new report"
    (with-fake-druid
      {(query-req-matching #"queryType.*timeBoundary") (druid-res "acceptance/time-boundary")
       (query-req-matching #"queryType.*timeseries") (druid-res "acceptance/totals")}
      (go-to-viewer)
      (click {:id "cb-measure-added"})
      (is (has-css? ".statistic" :count 2))
      (click-link "Reports")
      (click-link "Save")
      (fill-multi {{:name "name"} "The Amazing Report"
                        {:name "description"} "Something"})
      (click-link "Save")
      (is (has-css? "#notification" :text "Report 'The Amazing Report' saved!")))))
