(ns shevek.pages.designer.page
  (:require [reagent.core :as r]
            [shevek.reflow.db :as db]
            [shevek.i18n :refer [t]]
            [shevek.components.layout :refer [topbar]]
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
            [shevek.pages.designer.actions.maximize :refer [maximize-button]]
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

; TODO DASHBOARD usar esto en lugar de el dispatch directo en todos lados, y cambiar la condicion para q refresque periodicamente los cubos
(defn- cube-fetcher [cube-name render-fn]
  (when-not (db/get :cubes)
    (dispatch :cubes/fetch))
  (fn []
    (when-let [cube (db/get-in [:cubes cube-name])]
      (render-fn cube))))

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
     [report-builder report cube ; Build the new report unless already built
      (fn [report]
        [designer-builder (assoc props :report report :cube cube) ; Build the designer and put it in the app-db
         (fn [designer]
           [designer-renderer designer])])])])

(defn page
  "For creating or editing reports directly"
  []
  (let [{:keys [name] :or {name (:name @requested-report)} :as report} (db/get-in [:designer :report])]
    [:div#designer
     [topbar {:left [:h3.ui.inverted.header name]
              :right [:<>
                      [save-button report]
                      [maximize-button]
                      [refresh-button]]}]
     (when @requested-report ; Is nil while fetching a report by id
       [designer {:report @requested-report}])]))

(defn slave-designer
  "A designer whose report is owned by another component (a dashboard)"
  [{:keys [report] :as props}]
  [:div#designer
   [topbar {:left [:h3.ui.inverted.header (:name report)]
            :right [:<>
                    [:button.ui.icon.green.button
                     {:on-click #(.back js/history)}
                     [:i.reply.icon]]]}]
   [designer props]])
