(ns shevek.menu.reports
  (:require-macros [shevek.reflow.macros :refer [defevh]])
  (:require [reagent.core :as r]
            [shevek.reflow.db :as db]
            [shevek.reflow.core :refer [dispatch]]
            [shevek.rpc :as rpc]
            [shevek.i18n :refer [t]]
            [shevek.components.popup :refer [show-popup close-popup]]
            [shevek.components.form :refer [kb-shortcuts input-field hold-to-confirm]]
            [shevek.navigation :refer [current-page? navigate]]
            [shevek.notification :refer [notify]]
            [shevek.schemas.conversion :refer [viewer->report]]
            [shevek.viewer.page :as viewer]
            [shevek.lib.util :refer [new-record?]]
            [shevek.home.dashboards :refer [fetch-dashboards]]
            [cuerdas.core :as str]))

(defevh :reports-requested [db]
  (rpc/fetch db :reports "reports.api/find-all"))

(defn fetch-reports []
  (dispatch :reports-requested))

(defevh :report-selected [db {:keys [cube] :as report}]
  (viewer/prepare-cube db cube report))

(defevh :report-saved [db {:keys [name] :as report} editing-current? after-save]
  (after-save)
  (notify (t :reports/saved name))
  (fetch-reports)
  (when (current-page? :home) (fetch-dashboards))
  (cond-> (rpc/loaded db :saving-report)
          editing-current? (assoc :current-report report)))

(defn- current-report? [db report]
  (= (:id report) (get-in db [:current-report :id])))

(defevh :save-report [db report after-save]
  (let [editing-current? (or (new-record? report)
                             (and (current-report? db report) (current-page? :viewer)))
        report (if editing-current?
                 (merge report (viewer->report (db :viewer)))
                 report)]
    (rpc/call "reports.api/save-report" :args [report] :handler #(dispatch :report-saved % editing-current? after-save))
    (rpc/loading db :saving-report)))

(defevh :delete-report [db {:keys [name] :as report}]
  (rpc/call "reports.api/delete-report" :args [report]
            :handler (fn []
                       (notify (t :reports/deleted name))
                       (fetch-reports)
                       (fetch-dashboards)))
  db)

(defn- save-report-form [form-data]
  (let [valid? #(seq (:name @form-data))
        cancel #(reset! form-data nil)
        save #(when (valid?) (dispatch :save-report @form-data (fn [] (close-popup) (cancel))));
        shortcuts (kb-shortcuts :enter save :escape cancel)]
    (fn []
      [:div.ui.form (assoc (rpc/loading-class :saving-report) :ref shortcuts)
       [input-field form-data :name {:label (t :reports/name) :class "required" :auto-focus true}]
       [input-field form-data :description {:label (t :reports/description) :as :textarea :rows 2}]
       [input-field form-data :dashboards-ids {:label (t :reports/dashboards) :as :select-multiple
                                               :collection (map (juxt :name :id) (db/get :dashboards))}]
       [:button.ui.primary.button {:on-click save :class (when-not (valid?) "disabled")} (t :actions/save)]
       [:button.ui.button {:on-click cancel} (t :actions/cancel)]])))

(defn report-actions [report form-data]
  [:div.item-actions {:on-click #(.stopPropagation %)}
   [:i.write.icon {:on-click #(reset! form-data report)
                   :title (t :actions/edit)}]
   [:i.trash.icon (hold-to-confirm #(dispatch :delete-report report))]])

(defn- report-item [_ form-data]
  (let [select-report #(do (dispatch :report-selected %) (close-popup))]
    (fn [{:keys [_id name description cube] :as report} _]
      [:div.item {:on-click #(select-report report)}
       [:div.right.floated.content
        [:div.cube (db/get-in [:cubes cube :title])]]
       [:div.header name]
       [:div.description description]])))

(defn- reports-list [form-data]
  (let [current-report (or (db/get :current-report) {})
        reports (db/get :reports)
        show-actions? (current-page? :viewer)
        save #(if (new-record? current-report)
                (reset! form-data current-report)
                (dispatch :save-report current-report close-popup))
        save-as #(reset! form-data (dissoc current-report :id :name))
        loading? (when (rpc/loading? :saving-report) "loading")]
    [:div
     (when show-actions?
       [:div.actions
        [:button.ui.compact.green.button {:on-click save :class loading?} (t :actions/save)]
        [:button.ui.basic.compact.button {:on-click save-as} (t :actions/save-as)]])
     [:h3.ui.sub.orange.header {:class (when show-actions? "has-actions")} (t :reports/title)]
     (if (seq reports)
       [:div.ui.relaxed.middle.aligned.selection.list
        (for [report reports]
          ^{:key (:id report)} [report-item report form-data])]
       [:div (t :errors/no-results)])]))

(defn- popup-content []
  (let [form-data (r/atom nil)]
    (fn []
      [:div#reports-popup
       (if @form-data
         [save-report-form form-data]
         [reports-list form-data])])))

(defn- reports-menu []
  (when-not (current-page? :home) ; No need to fetch the reports again when we are on the home page
    (fetch-reports))
  (fn []
    (let [report-name (str/prune (db/get-in [:current-report :name]) 30)]
      [:a.item {:on-click #(show-popup % popup-content {:position "bottom left"})}
       [:i.line.chart.icon] (or (and (current-page? :viewer) report-name) (t :reports/title))])))
