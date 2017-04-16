(ns shevek.viewer.page
  (:require-macros [reflow.macros :refer [defevh]])
  (:require [reflow.core :refer [dispatch]]
            [reflow.db :as db]
            [shevek.rpc :as rpc]
            [shevek.dw :as dw]
            [shevek.navegation :refer [current-page? navigate]]
            [shevek.lib.util :refer [every]]
            [shevek.viewer.shared :refer [send-main-query current-cube-name]]
            [shevek.viewer.dimensions :refer [dimensions-panel]]
            [shevek.viewer.measures :refer [measures-panel]]
            [shevek.viewer.filter :refer [filter-panel]]
            [shevek.viewer.split :refer [split-panel]]
            [shevek.viewer.visualization :refer [visualization-panel]]
            [shevek.viewer.pinboard :refer [pinboard-panels send-pinboard-queries]]
            [shevek.schemas.conversion :refer [build-new-viewer report->viewer]]
            [shevek.reports.url :refer [restore-report-from-url]]))

(defn- init-viewer [cube current-report]
  (if current-report
    (report->viewer current-report cube)
    (build-new-viewer cube)))

(defevh :cube-arrived [{:keys [current-report] :as db} {:keys [name] :as cube}]
  (let [cube (dw/set-cube-defaults cube)
        {:keys [pinboard] :as viewer} (init-viewer cube current-report)]
    (-> (assoc db :viewer viewer)
        (rpc/loaded :cube-metadata)
        (send-main-query)
        (send-pinboard-queries))))

(defevh :cube-selected [db cube]
  (navigate "/viewer")
  (rpc/call "schema.api/cube" :args [cube] :handler #(dispatch :cube-arrived %))
  (-> (assoc db :viewer {:cube {:name cube}}) ; So we can display immediately the cube in the menu
      (dissoc :current-report)
      (rpc/loading :cube-metadata)))

(defevh :viewer-restored [db encoded-report]
  (if-let [{:keys [cube] :as report} (restore-report-from-url encoded-report)]
    (do
      (dispatch :navigate :viewer)
      (rpc/call "schema.api/cube" :args [cube] :handler #(dispatch :cube-arrived %))
      (-> (assoc db :viewer {:cube {:name cube}} :current-report report)
          (rpc/loading :cube-metadata)))
    (do
      (navigate "/")
      db)))

(defevh :max-time-arrived [db cube-name max-time]
  (update-in db [:cubes cube-name] assoc :max-time (dw/parse-max-time max-time)))

(defn fetch-max-time []
  (when (current-page? :viewer)
    (let [name (current-cube-name)]
      (rpc/call "schema.api/max-time" :args [name] :handler #(dispatch :max-time-arrived name %)))))

(defonce _interval (every 60 fetch-max-time))

(defn page []
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
