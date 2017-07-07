(ns shevek.acceptance.viewer-test
  (:require [clojure.test :refer :all]
            [shevek.acceptance.test-helper :refer :all]
            [etaoin.api :refer [refresh]]
            [shevek.support.druid :refer [with-fake-druid query-req-matching druid-res]]
            [shevek.support.viewer :refer [make-wikiticker-cube go-to-viewer]]))

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
