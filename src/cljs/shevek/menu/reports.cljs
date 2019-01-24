(ns shevek.menu.reports
  (:require [reagent.core :as r]
            [shevek.reflow.db :as db]
            [shevek.reflow.core :refer [dispatch] :refer-macros [defevh]]
            [shevek.rpc :as rpc]
            [shevek.i18n :refer [t]]
            [shevek.components.popup :refer [show-popup close-popup]]
            [shevek.components.form :refer [kb-shortcuts input-field]]
            [shevek.navigation :refer [current-page? navigate set-url]]
            [shevek.components.notification :refer [notify]]
            [shevek.lib.util :refer [new-record?]]
            [cuerdas.core :as str]))

(defevh :reports/fetch [db]
  (rpc/fetch db :reports "reports/find-all"))

(defn fetch-reports []
  (dispatch :reports/fetch))

(defevh :reports/saved [db {:keys [id name] :as report} after-save]
  (set-url (str "/reports/" id))
  (after-save)
  (notify (t :reports/saved name))
  (fetch-reports)
  (-> (rpc/loaded db :saving-report)
      (assoc-in [:designer :report] report)))

; TODO DASHBOARD esto era mucho mas complejo xq se usaba tb cdo se editaba desde el home. Para mi esa funcionalidad hay que quitarla y asi esto queda asi de simple. Que siempre se edite entrando al report. Ahora, recordar de quitar la edicion desde el home entonces.
(defevh :reports/save [db {:keys [id] :as form-data} after-save]
  (let [report (cond-> (merge (get-in db [:designer :report]) form-data)
                       (nil? id) (dissoc :id))] ; saving as?
    (rpc/call "reports/save-report" :args [report] :handler #(dispatch :reports/saved % after-save))
    (rpc/loading db :saving-report)))

; There may be a new dashbard in the collection if the form is open
(defn- save-report-form [form-data after-save]
  (let [valid? #(seq (:name @form-data))
        cancel #(reset! form-data nil)
        save #(when (valid?) (dispatch :reports/save @form-data (fn [] (after-save) (cancel))));
        shortcuts (kb-shortcuts :enter save :escape cancel)
        dashboards (->> (db/get :dashboards) (remove new-record?) (map (juxt :name :id)))]
    (fn []
      [:div.ui.form (assoc (rpc/loading-class :saving-report) :ref shortcuts)
       [input-field form-data :name {:label (t :reports/name) :class "required" :auto-focus true :on-focus #(-> % .-target .select)}]
       [input-field form-data :description {:label (t :reports/description) :as :textarea :rows 2}]
       [input-field form-data :dashboards-ids {:label (t :reports/dashboards) :as :select-multiple :collection dashboards}]
       [:button.ui.primary.button {:on-click save :class (when-not (valid?) "disabled")} (t :actions/save)]
       [:button.ui.button {:on-click cancel} (t :actions/cancel)]])))

(defn- report-item [{:keys [id name description cube]}]
  [:a.item {:href (str "/reports/" id) :on-click close-popup}
   [:div.right.floated.content
    [:div.cube (db/get-in [:cubes cube :title])]]
   [:div.header name]
   [:div.description description]])

(defn- reports-list [form-data]
  (let [current-report (db/get-in [:designer :report])
        reports (db/get :reports)
        show-actions? (current-page? :designer)
        save #(if (new-record? current-report)
                (reset! form-data current-report)
                (dispatch :reports/save current-report close-popup))
        save-as #(reset! form-data (assoc current-report :id nil :name nil))
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
          ^{:key (:id report)} [report-item report])]
       [:div (t :reports/missing)])]))

(defn- popup-content []
  (when-not (current-page? :home) ; No need to fetch the reports again when we are on the home page
    (fetch-reports))
  (fn []
    (let [form-data (r/atom nil)]
      (fn []
        [:div#reports-popup
         (if @form-data
           [save-report-form form-data close-popup]
           [reports-list form-data])]))))

(defn reports-menu []
  (let [report-name (str/prune (db/get-in [:designer :report :name]) 30)]
    [:a.item {:on-click #(show-popup % popup-content {:position "bottom left"})}
     [:i.line.chart.icon] (or (and (current-page? :designer) report-name) (t :reports/title))]))
