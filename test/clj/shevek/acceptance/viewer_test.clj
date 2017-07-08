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

  (testing "viewer restoration"
    (it "should restore viewer after page refresh" page
      (with-fake-druid
        {(query-req-matching #"queryType.*timeBoundary") (druid-res "acceptance/time-boundary")
         (query-req-matching #"queryType.*timeseries") (druid-res "acceptance/totals")}
        (go-to-viewer page)
        (is (has-css? page ".statistic" :count 3))
        (click page {:id "cb-measure-delta"})
        (is (has-css? page ".statistic" :count 4))
        (Thread/sleep 300) ; For the debounce
        (refresh page)
        (is (has-css? page ".statistic" :count 4))))

    (it "if the user paste the viewer URL when being unauthenticated, it should restore the viewer after login" page
      (with-fake-druid
        {(query-req-matching #"queryType.*timeBoundary") (druid-res "acceptance/time-boundary")
         (query-req-matching #"queryType.*timeseries") (druid-res "acceptance/totals")}
        (make-wikiticker-cube)
        (visit page "/#/viewer/ezpjdWJlICJ3aWtpdGlja2VyIiwgOm1lYXN1cmVzICgiZGVsZXRlZCIgImNvdW50IiksIDpmaWx0ZXIgKHs6bmFtZSAiX190aW1lIiwgOnBlcmlvZCAibGF0ZXN0LWRheSJ9KSwgOnNwbGl0ICgpLCA6cGluYm9hcmQgezptZWFzdXJlICJkZWxldGVkIiwgOmRpbWVuc2lvbnMgKCl9fQ==")
        (login page)
        (is (has-css? page ".statistic" :count 2))))

    (it "if the URL has been tampered, should navigate to home page" page
      (with-fake-druid
        {(query-req-matching #"queryType.*timeBoundary") (druid-res "acceptance/time-boundary")
         (query-req-matching #"queryType.*timeseries") (druid-res "acceptance/totals")}
        (login page)
        (visit page "/#/viewer/CHANGEDezpjdWJlICJ3aWtpdGlja2VyIiwgOm1lYXN1cmVzICgiZGVsZXRlZCIgImNvdW50IiksIDpmaWx0ZXIgKHs6bmFtZSAiX190aW1lIiwgOnBlcmlvZCAibGF0ZXN0LWRheSJ9KSwgOnNwbGl0ICgpLCA6cGluYm9hcmQgezptZWFzdXJlICJkZWxldGVkIiwgOmRpbWVuc2lvbnMgKCl9fQ==")
        (is (has-title? page "Dashboard"))))))
