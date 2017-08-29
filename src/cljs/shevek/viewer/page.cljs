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
            [shevek.viewer.viztype :refer [viztype-selector]]
            [shevek.viewer.pinboard :refer [pinboard-panels]]
            [shevek.schemas.conversion :refer [build-new-viewer report->viewer]]
            [shevek.viewer.url :refer [store-viewer-in-url restore-report-from-url]]
            [shevek.viewer.raw]
            [shevek.notification :refer [notify]]
            [shevek.i18n :refer [t]]))

(defn- already-build? [viewer]
  (some? (:measures viewer)))

(defn- init-viewer [cube current-viewer current-report]
  (cond
    (already-build? current-viewer) current-viewer ; Should only happen on dev when boot-reload refresh the page. Without it the viewer would be recreated everytime difficulting development
    current-report (report->viewer current-report cube)
    :else (build-new-viewer cube)))

(defevh :cube-arrived [{:keys [viewer current-report] :as db} {:keys [name] :as cube}]
  (let [cube (set-cube-defaults cube)
        db (rpc/loaded db :cube-metadata)
        {:keys [measures] :as viewer} (init-viewer cube viewer current-report)]
    (if (seq measures)
      (-> (assoc db :viewer viewer)
          (send-main-query)
          (send-pinboard-queries)
          (store-viewer-in-url))
      (do
        (notify (t :viewer/unauthorized (:title cube)) :type :error :timeout 5000)
        (navigate "/")))))

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
    [:div.top-row
     [:div.filter-split
       [filter-panel]
       [split-panel]]
     [viztype-selector]]
    [visualization-panel]]
   [:div.right-column
    [pinboard-panels]]])
