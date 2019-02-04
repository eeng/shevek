(ns shevek.pages.designer.page
  (:require [shevek.reflow.db :as db]
            [shevek.i18n :refer [t]]
            [shevek.components.layout :refer [topbar]]
            [shevek.pages.designer.dimensions :refer [dimensions-panel]]
            [shevek.pages.designer.measures :refer [measures-panel]]
            [shevek.pages.designer.filters :refer [filters-panel]]
            [shevek.pages.designer.splits :refer [splits-panel]]
            [shevek.pages.designer.viztype :refer [viztype-selector]]
            [shevek.pages.designer.visualization :refer [visualization-panel]]
            [shevek.pages.designer.pinboard :refer [pinboard-panel]]
            [shevek.pages.designer.helpers :refer [get-cube build-new-report send-designer-query send-pinboard-queries]]
            [shevek.schemas.conversion :refer [report->designer]]
            [shevek.reflow.core :refer [dispatch] :refer-macros [defevh]]
            [shevek.rpc :as rpc]))

(defevh :designer/new-report [db cube]
  (-> (assoc db :page :designer)
      (update :designer merge {:built? false :report {:cube cube :name (t :reports/new)}})))

(defevh :designer/report-arrived [db report]
  (assoc-in db [:designer :report] report))

(defevh :designer/edit-report [db id]
  (rpc/call "reports/find-by-id" :args [id] :handler #(dispatch :designer/report-arrived %))
  (-> (assoc db :page :designer)
      (assoc-in [:designer :built?] false)
      (update :designer dissoc :report)))

(defevh :designer/init [db {cube-name :cube :keys [viztype] :as report} {:keys [on-report-change] :or {on-report-change identity}}]
  (let [cube (get-in db [:cubes cube-name])
        report (if viztype report (build-new-report cube))
        designer (-> (report->designer report cube)
                     (assoc :report report
                            :on-report-change on-report-change
                            :built? true))]
    (-> (assoc db :designer designer)
        (send-designer-query)
        (send-pinboard-queries))))

(defn- render-designer [report opts]
  (dispatch :designer/init report opts)
  (fn [{:keys [name]} _]
    (let [{:keys [built? maximized] :as designer} (db/get :designer)
          maximized-class {:class (when maximized "hide")}]
      [:div#designer
       [topbar
        :left [:h3.ui.inverted.header name]
        :right [:<>
                [:button.ui.icon.button
                 [:i.save.icon]]
                [:button.ui.icon.button
                 [:i.refresh.icon]]]]

       (when built? ; The first render occours before the :designer/init when the designer is not built yet
         [:div.body
          [:div.left-column maximized-class
           [dimensions-panel]
           [measures-panel designer]]
          [:div.center-column
           [:div.top-row maximized-class
            [:div.filter-split
             [filters-panel designer]
             [splits-panel designer]]
            [viztype-selector designer]]
           [:div.bottom-row.panel
            [visualization-panel designer]]]
          [:div.right-column maximized-class
           [pinboard-panel designer]]])])))

(defn designer [{:keys [cube] :as report} opts]
  (when (get-cube cube) ; When entering directly to the designer via URL, the cube metadata has not arrived yet. Also when navigating to a report, we don't have the cube name until the report arrive.
    ^{:key cube} [render-designer report opts])) ; The key force the component to remount if the user switches between cubes from the menu, so that way the event :designer/init gets fired again.

; For creating or editing reports directly
(defn page []
  (dispatch :cubes/fetch) ; When entering via URL
  (fn []
    [designer (db/get-in [:designer :report])]))
