(ns shevek.acceptance.dashboard-test
  (:require [clojure.test :refer [deftest use-fixtures is]]
            [shevek.acceptance.test-helper :refer [wrap-acceptance-tests click has-css? has-no-css? it has-title? has-text? login visit click-tid fill wait-exists refresh element-value refresh click-text double-click]]
            [shevek.support.druid :refer [with-fake-druid query-req-matching druid-res]]
            [shevek.support.designer :refer [make-wikiticker-cube]]
            [shevek.makers :refer [make make!]]
            [shevek.schemas.dashboard :refer [Dashboard]]
            [shevek.schemas.report :refer [Report]]
            [etaoin.keys :as k]))

(use-fixtures :once wrap-acceptance-tests)

(deftest ^:acceptance dashboard-tests
  (it "create an empty dashboard a list it"
    (login)
    (click-tid "sidebar-dashboards")
    (is (has-title? "Dashboards"))
    (click-text "Create")
    (is (has-css? ".topbar-header" :text "New Dashboard"))
    (click-tid "save")
    (is (has-css? ".modal.visible"))
    (fill {:name "name"} "Superdash")
    (click-text "Save")
    (is (has-css? "#notification" :text "Dashboard saved!"))
    (is (has-css? ".topbar-header" :text "Superdash"))
    (is (has-no-css? ".modal.visible"))
    (click-tid "sidebar-dashboards")
    (is (has-text? "Superdash")))

  (it "adding a report"
    (with-fake-druid
      {(query-req-matching #"queryType.*timeseries") (druid-res "acceptance/totals")}
      (make-wikiticker-cube)
      (login)

      (click-text "Create")
      (click-text "Add Panel")
      (is (has-css? ".panel" :count 1))
      (is (has-css? ".panel" :text "Select a cube"))
      (click-text "Wikiticker")
      (is (has-css? ".statistic" :count 3))

      (click-tid "edit-panel")
      (is (has-css? "#designer"))
      (is (has-css? ".statistic" :count 3))
      (click {:fn/text "Deleted"})
      (is (has-css? ".statistic" :count 2))

      (click-tid "go-back")
      (is (has-css? "#dashboard"))
      (is (has-css? ".statistic" :count 2))

      (click-tid "save")
      (is (has-css? ".modal.visible"))
      (fill {:name "name"} "Two Panels")
      (click-text "Save")
      (is (has-css? "#notification" :text "Dashboard saved!"))
      (refresh)
      (is (has-css? ".panel" :count 1))
      (is (has-css? ".statistic" :count 2))))

  (it "renaming a dashboard and his reports"
    (with-fake-druid
      {(query-req-matching #"queryType.*timeseries") (druid-res "acceptance/totals")}
      (make-wikiticker-cube)
      (let [user (login)
            report (make Report {:name "New Report" :cube "wikiticker" :measures ["count"]})]
        (make! Dashboard {:user-id (:id user) :name "New Dashboard"
                          :panels [{:type "report" :report report :grid-pos {:x 0 :y 0 :w 10 :h 10}}]}))
      (click-tid "sidebar-dashboards")
      (click-text "New Dashboard")
      (is (has-css? ".statistic" :count 1))

      (click-tid "edit-panel")
      (is (has-css? ".statistic" :count 1))
      (double-click ".pencil")
      (fill {:css ".transparent input"} (k/with-shift k/home) k/delete "SuperReport" k/enter)
      (click-tid "go-back")
      (is (has-css? ".panel" :text "SuperReport"))

      (double-click ".pencil")
      (fill {:css ".transparent input"} (k/with-shift k/home) k/delete "MegaDash" k/enter)
      (click-tid "save")
      (is (has-css? "#notification" :text "Dashboard saved!"))

      (refresh)
      (is (has-css? ".panel" :text "SuperReport"))
      (is (has-css? ".topbar-header" :text "MegaDash")))))
