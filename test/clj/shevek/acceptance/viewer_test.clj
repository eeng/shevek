(ns shevek.acceptance.viewer-test
  (:require [clojure.test :refer :all]
            [shevek.acceptance.test-helper :refer :all]
            [shevek.support.druid :refer [with-fake-druid query-req-matching druid-res]]
            [shevek.support.viewer :refer [make-wikiticker-cube go-to-viewer]]))

(use-fixtures :once wrap-acceptance-tests)

(deftest ^:acceptance viewer
  (it "shows dimensions, measures and basic stats on entry"
    (with-fake-druid
      {(query-req-matching #"queryType.*timeBoundary") (druid-res "acceptance/time-boundary")
       (query-req-matching #"queryType.*timeseries") (druid-res "acceptance/totals")}
      (go-to-viewer)
      (is (has-css? ".statistic" :count 3))
      (is (has-css? ".statistics" :text "394298"))
      (is (has-css? ".dimensions" :text "Region Name"))
      (is (has-css? ".measures" :text "User Unique"))))

  (testing "viewer restoration"
    (it "should restore viewer after refresh"
      (with-fake-druid
        {(query-req-matching #"queryType.*timeBoundary") (druid-res "acceptance/time-boundary")
         (query-req-matching #"queryType.*timeseries") (druid-res "acceptance/totals")}
        (go-to-viewer)
        (is (has-css? ".statistic" :count 3))
        (click {:id "cb-measure-delta"})
        (is (has-css? ".statistic" :count 4))
        (refresh)
        (is (has-css? ".statistic" :count 4))))

    (it "if the URL has been tampered, should navigate to home"
      (with-fake-druid
        {(query-req-matching #"queryType.*timeBoundary") (druid-res "acceptance/time-boundary")
         (query-req-matching #"queryType.*timeseries") (druid-res "acceptance/totals")}
        (login)
        (visit "/viewer/CHANGEDezpjdWJlICJ3aWtpdGlja2VyIiwgOm1lYXN1cmVzICgiZGVsZXRlZCIgImNvdW50IiksIDpmaWx0ZXIgKHs6bmFtZSAiX190aW1lIiwgOnBlcmlvZCAibGF0ZXN0LWRheSJ9KSwgOnNwbGl0ICgpLCA6cGluYm9hcmQgezptZWFzdXJlICJkZWxldGVkIiwgOmRpbWVuc2lvbnMgKCl9fQ==")
        (is (has-title? "Welcome")))))

  (it "raw data modal"
    (with-fake-druid
      {(query-req-matching #"queryType.*timeBoundary") (druid-res "acceptance/time-boundary")
       (query-req-matching #"queryType.*timeseries") (druid-res "acceptance/totals")
       (query-req-matching #"queryType.*select") (druid-res "acceptance/select")}
      (go-to-viewer)
      (click-link "Share")
      (click-link "View raw data")
      (is (has-css? ".raw-data" :text "Showing the first 100 events matching: Latest Day"))
      (is (has-css? ".raw-data tbody tr" :count 2))
      (visit "/"))))
