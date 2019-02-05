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
            [shevek.schemas.conversion :refer [report->designer]]
            [shevek.reflow.core :refer [dispatch] :refer-macros [defevh]]
            [shevek.rpc :as rpc]))

(defevh :designer/new-report [db cube]
  (assoc db :page :designer
            :designer {:built? false :report {:cube cube :name (t :reports/new)}}))

(defevh :designer/report-arrived [db report]
  (assoc-in db [:designer :report] report))

(defevh :designer/edit-report [db id]
  (rpc/call "reports/find-by-id" :args [id] :handler #(dispatch :designer/report-arrived %))
  (assoc db :page :designer
            :designer {:built? false}))

(defn- designer-renderer [{:keys [maximized measures filters splits viztype pinboard report report-results]}]
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
      [pinboard-panel pinboard]]]))

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

(defevh :designer/build [db {:keys [report cube on-report-change] :or {on-report-change identity}}]
  (let [designer (-> (report->designer report cube)
                     (assoc :report report
                            :on-report-change on-report-change
                            :built? true))]
    (-> (assoc db :designer designer)
        (send-designer-query)
        (send-pinboard-queries))))

(defevh :designer/unbuild [db]
  (assoc-in db [:designer :built?] false))

(defn- designer-builder [props render-fn]
  (r/create-class
   {:reagent-render (fn []
                      (let [{:keys [built?] :as designer} (db/get :designer)]
                        (when built?
                          (render-fn designer))))
    :component-did-mount #(dispatch :designer/build props)
    :component-will-unmount #(dispatch :designer/unbuild)}))

(defn- designer [{:keys [report] :as props}]
  (when report ; Is nil while fetching a report by id
    [cube-fetcher (:cube report) ; Wait until the cube metadata is ready
     (fn [cube]
       [report-builder report cube ; Build the new report unless already built
        (fn [report]
          [designer-builder (assoc props :report report :cube cube) ; Build the designer and put it in the app-db
           (fn [designer]
             [designer-renderer designer])])])]))

(defn page
  "For creating or editing reports directly"
  []
  (let [report (db/get-in [:designer :report])]
    [:div#designer
     [topbar {:left [:h3.ui.inverted.header (:name report)]
              :right [:<>
                      [:button.ui.icon.button
                       [:i.save.icon]]
                      [:button.ui.icon.button
                       [:i.refresh.icon]]]}]
     [designer {:report report}]]))
