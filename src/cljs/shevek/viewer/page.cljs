(ns shevek.viewer.page
  (:require-macros [reflow.macros :refer [defevh]])
  (:require [reflow.core :refer [dispatch]]
            [reflow.db :as db]
            [shevek.rpc :as rpc]
            [shevek.navegation :refer [current-page? navigate]]
            [shevek.lib.util :refer [every]]
            [shevek.lib.dw.cubes :refer [set-cube-defaults]]
            [shevek.lib.dw.time :refer [parse-max-time]]
            [shevek.viewer.shared :refer [send-main-query send-pinboard-queries current-cube-name]]
            [shevek.viewer.dimensions :refer [dimensions-panel]]
            [shevek.viewer.measures :refer [measures-panel]]
            [shevek.viewer.filter :refer [filter-panel]]
            [shevek.viewer.split :refer [split-panel]]
            [shevek.viewer.visualization :refer [visualization-panel]]
            [shevek.viewer.pinboard :refer [pinboard-panels]]
            [shevek.schemas.conversion :refer [build-new-viewer report->viewer]]
            [shevek.reports.url :refer [restore-report-from-url]]))

(defn- init-viewer [cube current-report]
  (if current-report
    (report->viewer current-report cube)
    (build-new-viewer cube)))

(defevh :cube-arrived [{:keys [current-report] :as db} {:keys [name] :as cube}]
  (let [cube (set-cube-defaults cube)
        {:keys [pinboard] :as viewer} (init-viewer cube current-report)]
    (-> (assoc db :viewer viewer)
        (rpc/loaded :cube-metadata)
        (send-main-query)
        (send-pinboard-queries))))

(defevh :viewer-initialized [db]
  (rpc/call "schema.api/cube" :args [(get-in db [:viewer :cube :name])] :handler #(dispatch :cube-arrived %))
  (rpc/loading db :cube-metadata))

(defn prepare-cube [db cube report]
  (if (current-page? :viewer)
    (dispatch :viewer-initialized)
    (navigate "/viewer"))
  (assoc db :viewer {:cube {:name cube}} :current-report report)) ; So we can display immediately the cube in the menu

(defevh :cube-selected [db cube]
  (prepare-cube db cube nil))

(defevh :viewer-restored [db encoded-report]
  (if-let [{:keys [cube] :as report} (restore-report-from-url encoded-report)]
    (prepare-cube db cube report)
    (do
      (navigate "/")
      db)))

(defevh :max-time-arrived [db max-time]
  (assoc-in db [:viewer :cube :max-time] (parse-max-time max-time)))

(defn fetch-max-time []
  (when (current-page? :viewer)
    (let [name (current-cube-name)]
      (rpc/call "schema.api/max-time" :args [name] :handler #(dispatch :max-time-arrived %)))))

(defonce _interval (every 60 fetch-max-time))

(defevh :viewer/refresh [db]
  (-> (send-main-query db)
      (send-pinboard-queries)))

(defn page []
  (dispatch :viewer-initialized)
  [:div#viewer
   [:div.left-column
    [dimensions-panel]
    [measures-panel]]
   [:div.center-column
    [:div
     [filter-panel]
     [split-panel]]
    [visualization-panel]]
   [:div.right-column
    [pinboard-panels]]])
