(ns shevek.acceptance.reports-test
  (:require [clojure.test :refer [deftest use-fixtures is]]
            [shevek.acceptance.test-helper :refer [wrap-acceptance-tests it click click-link fill has-css? click-tid has-text? wait-exists]]
            [shevek.support.druid :refer [with-fake-druid query-req-matching druid-res]]
            [shevek.support.designer :refer [go-to-designer]]
            [etaoin.keys :as keys]))

(use-fixtures :once wrap-acceptance-tests)

(deftest ^:acceptance reports
  (it "creating new report"
    (with-fake-druid
      {(query-req-matching #"queryType.*timeBoundary") (druid-res "acceptance/time-boundary")
       (query-req-matching #"queryType.*timeseries") (druid-res "acceptance/totals")}

      (go-to-designer)
      (is (has-css? ".statistic" :count 3))
      (click {:id "cb-measure-added"})
      (is (has-css? ".statistic" :count 2))
      (click-tid "save")
      (fill {:name "name"} (keys/with-shift keys/home) keys/delete "The Amazing Report")
      (click-link "Save")
      (is (has-css? "#notification" :text "Report saved!"))
      (is (has-text? "The Amazing Report"))

      (wait-exists {:css "#notification.hidden"})
      (click-tid "save") ; This saves should open the Save As dialog again
      (is (has-css? "#notification" :text "Report saved!")))))
