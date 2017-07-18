(ns shevek.viewer.page
  (:require-macros [shevek.reflow.macros :refer [defevh defevhi]])
  (:require [shevek.reflow.core :refer [dispatch]]
            [shevek.reflow.db :as db]
            [shevek.rpc :as rpc]
            [shevek.navegation :refer [current-page? navigate]]
            [shevek.lib.dw.cubes :refer [set-cube-defaults]]
            [shevek.lib.dates :refer [parse-time]]
            [shevek.viewer.shared :refer [send-main-query send-pinboard-queries current-cube-name]]
            [shevek.viewer.dimensions :refer [dimensions-panel]]
            [shevek.viewer.measures :refer [measures-panel]]
            [shevek.viewer.filter :refer [filter-panel]]
            [shevek.viewer.split :refer [split-panel]]
            [shevek.viewer.visualization :refer [visualization-panel]]
            [shevek.viewer.pinboard :refer [pinboard-panels]]
            [shevek.schemas.conversion :refer [build-new-viewer report->viewer]]
            [shevek.reports.url :refer [store-viewer-in-url restore-report-from-url]]))

(defn- init-viewer [cube current-report]
  (if current-report
    (report->viewer current-report cube)
    (build-new-viewer cube)))

(defevhi :cube-arrived [{:keys [current-report] :as db} {:keys [name] :as cube}]
  {:after [store-viewer-in-url]}
  (let [cube (set-cube-defaults cube)
        {:keys [pinboard] :as viewer} (init-viewer cube current-report)]
    (-> (assoc db :viewer viewer)
        (rpc/loaded :cube-metadata)
        (send-main-query)
        (send-pinboard-queries))))

(defevh :viewer-initialized [db]
  (if-let [cube (get-in db [:viewer :cube :name])]
    (do ; TODO las mimas tres lineas
      (rpc/call "schema.api/cube" :args [cube] :handler #(dispatch :cube-arrived %))
      (rpc/loading db :cube-metadata))
    (navigate "/")))

(defn prepare-cube [db cube report]
  (if (current-page? :viewer)
    (dispatch :viewer-initialized)
    (navigate "/viewer"))
  (assoc db :viewer {:cube {:name cube}} :current-report report)) ; So we can display immediately the cube in the menu

(defevh :cube-selected [db cube]
  (prepare-cube db cube nil))

(defevh :viewer-restored [db encoded-report]
  (let [{:keys [cube] :as report} (restore-report-from-url encoded-report)]
    (prepare-cube db cube report)))

(defevh :max-time-arrived [db max-time]
  (if-let [new-data (> (parse-time max-time) (get-in db [:viewer :cube :max-time]))]
    (-> (assoc-in db [:viewer :cube :max-time] (parse-time max-time))
        (send-main-query)
        (send-pinboard-queries))))

(defevh :viewer-refresh [db]
  (when-not (rpc/loading?)
    (rpc/call "schema.api/max-time" :args [(current-cube-name)] :handler #(dispatch :max-time-arrived %))))

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
