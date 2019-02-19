(ns shevek.acceptance.designer-test
  (:require [clojure.test :refer [deftest use-fixtures is]]
            [shevek.acceptance.test-helper :refer [wrap-acceptance-tests click click-text has-css? it has-title? has-text? login visit click-tid fill wait-exists refresh element-value has-no-text?]]
            [shevek.support.druid :refer [with-fake-druid query-req-matching druid-res]]
            [shevek.support.designer :refer [make-wikiticker-cube go-to-designer]]
            [shevek.makers :refer [make!]]
            [shevek.schemas.report :refer [Report]]
            [etaoin.keys :as keys]))

(use-fixtures :once wrap-acceptance-tests)

(deftest ^:acceptance designer-tests
  (it "shows dimensions, measures and basic stats on entry"
    (with-fake-druid
      {(query-req-matching #"queryType.*timeseries") (druid-res "acceptance/totals")}
      (go-to-designer)
      (is (has-css? ".statistic" :count 3))
      (is (has-css? ".statistics" :text "394298"))
      (is (has-css? ".dimensions" :text "Region Name"))
      (is (has-css? ".measures" :text "User Unique"))))

  (it "add a split"
    (with-fake-druid
      {(query-req-matching #"queryType.*timeseries") (druid-res "acceptance/totals")
       (query-req-matching #"queryType.*topN") (druid-res "acceptance/topn")}
      (go-to-designer)
      (click {:css ".dimensions .item:nth-child(2)"})
      (is (has-css? ".split .button" :count 0))
      (click {:data-tid "add-split"})
      (is (has-css? ".split .button" :count 1))
      (is (has-css? ".visualization table tbody tr" :count 51))
      (is (has-css? ".visualization" :text "City Name"))
      (is (has-css? ".visualization" :text "Buenos Aires"))))

  (it "raw data modal"
    (with-fake-druid
      {(query-req-matching #"queryType.*timeseries") (druid-res "acceptance/totals")
       (query-req-matching #"queryType.*select") (druid-res "acceptance/select")}
      (go-to-designer)
      (click-tid "raw-data")
      (is (has-css? ".raw-data" :text "Showing the first 100 events matching: Latest Day"))
      (is (has-css? ".raw-data tbody tr" :count 2))))

  (it "save as, save again and refresh page"
    (with-fake-druid
      {(query-req-matching #"queryType.*timeseries") (druid-res "acceptance/totals")}

      (go-to-designer)
      (is (has-css? ".statistic" :count 3))
      (click {:fn/text "Added"})
      (is (has-css? ".statistic" :count 2))
      (click-tid "save")
      (fill {:name "name"} (keys/with-shift keys/home) keys/delete "The Amazing Report")
      (click-text "Save")
      (is (has-css? "#notification" :text "Report saved!"))
      (is (has-text? "The Amazing Report"))

      (wait-exists {:css "#notification.hidden"})
      (click-tid "save") ; This saves should open the Save As dialog again
      (is (has-css? "#notification" :text "Report saved!"))

      (refresh)
      (is (has-css? ".statistic" :count 2))
      (is (has-text? "The Amazing Report"))))

  (it "allows to share a report"
    (with-fake-druid
      {(query-req-matching #"queryType.*timeseries") (druid-res "acceptance/totals")}
      (go-to-designer)
      (click-tid "share")
      (is (has-css? ".modal .header" :text "Share"))
      (is (re-matches #"http://localhost:4100/reports/.+"
                      (element-value {:css ".modal input"})))
      (click-text "Copy")
      (is (has-css? "#notification" :text "Link copied!"))))

  (it "authorization"
    (let [u (login {:allowed-cubes []})]
      (make! Report {:name "R1" :cube "wikiticker" :measures ["count"] :owner-id (:id u)})
      (is (has-text? "There are no cubes defined"))
      (refresh)
      (click-text "R1")
      (is (has-css? "#designer"))
      (is (has-css? ".page-message" :text "This cube is no longer available"))
      (is (has-no-text? "Count"))
      (is (has-no-text? "City")))))
