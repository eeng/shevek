(ns shevek.acceptance.reports-test
  (:require [clojure.test :refer :all]
            [shevek.acceptance.test-helper :refer :all]
            [shevek.support.druid :refer [with-fake-druid query-req-matching druid-res]]
            [shevek.support.viewer :refer [make-wikiticker-cube go-to-viewer]]))

(deftest reports
  (it "creating new report" page
    (with-fake-druid
      {(query-req-matching #"queryType.*timeBoundary") (druid-res "acceptance/time-boundary")
       (query-req-matching #"queryType.*timeseries") (druid-res "acceptance/totals")}
      (go-to-viewer page)
      (click page {:id "cb-measure-added"})
      (is (has-css? page ".statistic" :count 2))
      (click-link page "Reports")
      (click-link page "Save")
      (fill-multi page {{:name "name"} "The Amazing Report"
                        {:name "description"} "Something"})
      (click-link page "Save")
      (is (has-css? page "#notification" :text "Report 'The Amazing Report' saved!")))))
