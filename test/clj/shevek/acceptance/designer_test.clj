(ns shevek.acceptance.designer-test
  (:require [clojure.test :refer [deftest use-fixtures is]]
            [shevek.acceptance.test-helper :refer [wrap-acceptance-tests click click-link has-css? it has-title? login visit click-tid]]
            [shevek.support.druid :refer [with-fake-druid query-req-matching druid-res]]
            [shevek.support.designer :refer [make-wikiticker-cube go-to-designer]]))

(use-fixtures :once wrap-acceptance-tests)

(deftest ^:acceptance designer
  (it "shows dimensions, measures and basic stats on entry"
    (with-fake-druid
      {(query-req-matching #"queryType.*timeBoundary") (druid-res "acceptance/time-boundary")
       (query-req-matching #"queryType.*timeseries") (druid-res "acceptance/totals")}
      (go-to-designer)
      (is (has-css? ".statistic" :count 3))
      (is (has-css? ".statistics" :text "394298"))
      (is (has-css? ".dimensions" :text "Region Name"))
      (is (has-css? ".measures" :text "User Unique"))))

  (it "raw data modal"
    (with-fake-druid
      {(query-req-matching #"queryType.*timeBoundary") (druid-res "acceptance/time-boundary")
       (query-req-matching #"queryType.*timeseries") (druid-res "acceptance/totals")
       (query-req-matching #"queryType.*select") (druid-res "acceptance/select")}
      (go-to-designer)
      (click-tid "raw-data")
      (is (has-css? ".raw-data" :text "Showing the first 100 events matching: Latest Day"))
      (is (has-css? ".raw-data tbody tr" :count 2))
      (click-link "Close"))))
