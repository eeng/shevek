(ns shevek.viewer.page
  (:require [shevek.reflow.core :refer [dispatch] :refer-macros [defevh defevhi]]
            [shevek.reflow.db :as db]
            [shevek.rpc :as rpc]
            [shevek.navigation :refer [current-page? navigate]]
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
            [shevek.i18n :refer [t]]))

(defn- already-build? [viewer]
  (some? (:measures viewer)))

(defn- init-viewer [cube current-viewer current-report]
  (cond
    (already-build? current-viewer) current-viewer ; Should only happen on dev when the page is autorefreshed. Without it the viewer would be recreated everytime difficulting development
    current-report (report->viewer current-report cube)
    :else (build-new-viewer cube)))

(defn- cube-authorized? [{:keys [measures]}]
  (seq measures))

(defn cube-arrived [{:keys [viewer current-report] :as db} cube]
  (if (cube-authorized? cube)
    (let [viewer (init-viewer cube viewer current-report)]
      (-> (assoc db :viewer viewer)
          (send-main-query)
          (send-pinboard-queries)
          (store-viewer-in-url)))
    (dispatch :errors/show-page {:message (t :viewer/unauthorized (:title cube))})))

(defevh :viewer-initialized [db]
  (if-let [cube (get-in db [:viewer :cube :name])]
    (rpc/fetch db :cube-metadata "schema/cube" :args [cube] :handler cube-arrived)
    (navigate "/")))

(defn prepare-cube [db cube report]
  (if (current-page? :viewer)
    (dispatch :viewer-initialized)
    (dispatch :navigate :viewer))
  (assoc db :viewer {:cube {:name cube}} :current-report report))

(defevh :cube-selected [db cube]
  (prepare-cube db cube nil))

(defevh :viewer-restored [db encoded-report]
  (let [{:keys [cube] :as report} (restore-report-from-url encoded-report)]
    (prepare-cube db cube report)))

(defevh :max-time-arrived [db max-time]
  (if (> max-time (get-in db [:viewer :cube :max-time]))
    (-> (assoc-in db [:viewer :cube :max-time] max-time)
        (send-main-query)
        (send-pinboard-queries))))

(defevh :viewer/refresh [db]
  (when-not (rpc/loading?)
    (rpc/call "schema/max-time" :args [(current-cube-name)] :handler #(dispatch :max-time-arrived %))))

(defn page []
  (dispatch :viewer-initialized)
  (fn []
    (let [maximized? (db/get-in [:viewer :maximized])
          loading-metadata? (nil? (db/get-in [:viewer :cube :dimensions]))]
      (if loading-metadata?
        [:div.ui.active.large.loader.preloader]
        [:div#viewer
         (when-not maximized?
           [:div.left-column
            [dimensions-panel]
            [measures-panel]])
         [:div.center-column
          (when-not maximized?
            [:div.top-row
             [:div.filter-split
              [filter-panel]
              [split-panel]]
             [viztype-selector]])
          [:div.bottom-row.panel
           [visualization-panel]]]
         (when-not maximized?
           [:div.right-column
            [pinboard-panels]])]))))
