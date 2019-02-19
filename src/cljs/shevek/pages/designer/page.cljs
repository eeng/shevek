(ns shevek.pages.designer.page
  (:require [reagent.core :as r]
            [shevek.reflow.db :as db]
            [shevek.i18n :refer [t]]
            [shevek.components.layout :refer [topbar]]
            [shevek.components.message :refer [page-message]]
            [shevek.pages.cubes.helpers :refer [cube-fetcher cube-authorized?]]
            [shevek.pages.designer.dimensions :refer [dimensions-panel]]
            [shevek.pages.designer.measures :refer [measures-panel]]
            [shevek.pages.designer.filters :refer [filters-panel]]
            [shevek.pages.designer.splits :refer [splits-panel]]
            [shevek.pages.designer.viztype :refer [viztype-selector]]
            [shevek.pages.designer.visualization :refer [visualization-panel]]
            [shevek.pages.designer.pinboard :refer [pinboard-panel]]
            [shevek.pages.designer.helpers :refer [build-new-report send-designer-query send-pinboard-queries]]
            [shevek.pages.designer.actions.refresh :refer [refresh-button]]
            [shevek.pages.designer.actions.save :refer [save-button]]
            [shevek.pages.designer.actions.share :refer [share-button]]
            [shevek.pages.designer.actions.raw :refer [raw-data-button]]
            [shevek.pages.designer.actions.maximize :refer [maximize-button]]
            [shevek.pages.designer.actions.download :refer [download-csv-button]]
            [shevek.pages.designer.actions.go-back :refer [go-back-button]]
            [shevek.pages.designer.actions.rename :refer [report-name]]
            [shevek.schemas.conversion :refer [report->designer]]
            [shevek.reflow.core :refer [dispatch] :refer-macros [defevh]]
            [shevek.rpc :as rpc]))

(defonce requested-report (r/atom nil))

(defevh :designer/new-report [db cube]
  (reset! requested-report {:cube cube :name (t :reports/new)})
  (assoc db :page :designer))

(defevh :designer/edit-report [db id]
  (reset! requested-report nil)
  (rpc/call "reports/find-by-id" :args [id] :handler #(reset! requested-report %))
  (assoc db :page :designer))

(defn- designer-renderer [{:keys [maximized measures filters splits viztype report report-results] :as designer}]
  (let [maximized-class {:class (when maximized "hide")}]
    [:div.body
     [:div.left-column maximized-class
      [dimensions-panel]
      [measures-panel measures]]
     [:div.center-column
      [:div.top-row maximized-class
       [:div.filter-split
        [filters-panel filters]
        [splits-panel splits]]
       [viztype-selector viztype]]
      [:div.bottom-row.panel
       [visualization-panel report report-results]]]
     [:div.right-column maximized-class
      [pinboard-panel designer]]]))

(defn- report-builder [{:keys [measures] :as provisory-report} cube render-fn]
  (let [report (if measures
                 provisory-report
                 (build-new-report cube))]
    (render-fn report)))

(defevh :designer/build [db {:keys [report cube report-results on-report-change] :or {on-report-change identity}}]
  (let [designer (-> (report->designer report cube)
                     (assoc :report report
                            :on-report-change on-report-change
                            :report-results report-results))]
    (cond-> (assoc db :designer designer)
            (not report-results) (send-designer-query) ; No need to send the query again if we already have the results (which come from the dashboard)
            true (send-pinboard-queries))))

 ; We need to unbuild it so the next time the user enters, the previous designer doesn't show until the new one is built
(defevh :designer/unbuild [db]
  (dissoc db :designer))

(defn- designer-builder [props render-fn]
  (r/create-class
   {:reagent-render (fn []
                      (when-let [designer (db/get :designer)]
                        (render-fn designer)))
    :component-did-mount #(dispatch :designer/build props)
    :component-will-unmount #(dispatch :designer/unbuild)}))

(defn- designer
  "To render the designer we need a report, and to build a report we need the full cube,
   but the events that fetch them are async so can happen in any order.
   This component takes care of handling all this and only render the designer when everything is ready."
  [{:keys [report] :as props}]
  [cube-fetcher (:cube report) ; Wait until the cube metadata is ready
   (fn [cube]
     (if cube
       [report-builder report cube ; Build the new report unless already built
        (fn [report]
          [designer-builder (assoc props :report report :cube cube) ; Build the designer and put it in the app-db
           (fn [designer]
             [designer-renderer designer])])]
       [page-message {:header (t :cubes/unauthorized-title) :content (t :cubes/unauthorized)}]))])

(defn- designer-container [{initial-report :report :keys [actions] :as props}]
  (let [{:keys [report report-results]} (db/get :designer)]
    [:div#designer
     [topbar {:left [report-name (or report initial-report)]
              :right (when (cube-authorized? (:cube report))
                       (into [:<>] (actions {:report report :report-results report-results})))}]
     (when initial-report ; Is nil while fetching a report by id
       [designer (dissoc props :actions)])]))

(defn page
  "For creating or editing reports directly"
  []
  [designer-container
   {:report @requested-report
    :actions (fn [{:keys [report report-results]}]
               [[save-button report]
                [share-button report]
                [download-csv-button report report-results]
                [:div.divider]
                [raw-data-button]
                [maximize-button]
                [:div.divider]
                [refresh-button]])}])

(defn slave-designer
  "A designer whose report is owned by another component (a dashboard)"
  [props]
  [designer-container
   (assoc props
          :actions (fn [{:keys [report report-results]}]
                     [[share-button report]
                      [download-csv-button report report-results]
                      [:div.divider]
                      [raw-data-button]
                      [maximize-button]
                      [:div.divider]
                      [go-back-button]]))])
