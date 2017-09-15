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
            [cuerdas.core :as str]))

(defevh :reports-requested [db]
  (rpc/fetch db :reports "reports.api/find-all"))

(defn fetch-reports []
  (dispatch :reports-requested))

(defevh :report-selected [db {:keys [cube] :as report}]
  (viewer/prepare-cube db cube report))

(defevh :report-saved [db {:keys [name] :as report} editing-current?]
  (notify (t :reports/saved name))
  (fetch-reports)
  (cond-> (rpc/loaded db :save-report)
          editing-current? (assoc :current-report report)))

(defn- current-report? [db report]
  (= (:id report) (get-in db [:current-report :id])))

(defevh :save-report [db report]
  (let [editing-current? (or (new-record? report)
                             (and (current-report? db report) (current-page? :viewer)))
        report (if editing-current?
                 (merge report (viewer->report (db :viewer)))
                 report)]
    (rpc/call "reports.api/save-report" :args [report] :handler #(dispatch :report-saved % editing-current?))
    (rpc/loading db :save-report)))

(defevh :delete-report [db {:keys [name] :as report}]
  (rpc/call "reports.api/delete-report" :args [report]
            :handler #(do (notify (t :reports/deleted name)) (fetch-reports)))
  (cond-> (rpc/loading db :save-report)
          (current-report? db report) (dissoc :current-report)))

(defn- save-report-form [form-data after-save]
  (let [report (r/cursor form-data [:report])
        valid? #(seq (:name @report))
        cancel #(reset! form-data nil)
        save #(when (valid?)
                (dispatch :save-report @report)
                (after-save)
                (js/setTimeout cancel 100))
        shortcuts (kb-shortcuts :enter save :escape cancel)]
    (fn []
      [:div.ui.form {:ref shortcuts}
       [input-field report :name {:label (t :reports/name) :class "required" :auto-focus true}]
       [input-field report :description {:label (t :reports/description) :as :textarea :rows 2}]
       [input-field report :dashboards-ids {:label (t :reports/dashboards) :as :select-multiple
                                            :collection (map (juxt :name :id) (db/get :dashboards))}]
       [:button.ui.primary.button {:on-click save :class (when-not (valid?) "disabled")} (t :actions/save)]
       [:button.ui.button {:on-click cancel} (t :actions/cancel)]])))

(defn report-actions [report form-data]
  [:div.item-actions {:on-click #(.stopPropagation %)}
   [:i.write.icon {:on-click #(reset! form-data {:report report :editing? true})
                   :title (t :actions/edit)}]
   [:i.trash.icon (hold-to-confirm #(dispatch :delete-report report))]])

(defn- report-item [_ form-data]
  (let [select-report #(do (dispatch :report-selected %) (close-popup))]
    (fn [{:keys [_id name description cube] :as report} _]
      [:div.item {:on-click #(select-report report)}
       [:div.right.floated.content
        [:div.cube (db/get-in [:cubes cube :title])]
        [report-actions report form-data]]
       [:div.header name]
       [:div.description description]])))

(defn- reports-list [form-data]
  (let [current-report (or (db/get :current-report) {})
        reports (db/get :reports)
        show-actions? (current-page? :viewer)
        save #(if (new-record? current-report)
                (reset! form-data {:report current-report :editing? false})
                (do (dispatch :save-report current-report) (close-popup)))
        save-as #(reset! form-data {:report (dissoc current-report :id :name) :editing? false})]
    [:div
     (when show-actions?
       [:div.actions
        [:button.ui.compact.green.button {:on-click save} (t :actions/save)]
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
         [save-report-form form-data #(when-not (:editing? @form-data) (close-popup))]
         [reports-list form-data])])))

(defn- reports-menu []
  (when-not (current-page? :home) ; No need to fetch the reports again when we are on the home page
    (fetch-reports))
  (fn []
    (let [report-name (str/prune (db/get-in [:current-report :name]) 30)]
      [:a.item {:on-click #(show-popup % popup-content {:position "bottom left"})}
       [:i.line.chart.icon] (or (and (current-page? :viewer) report-name) (t :reports/title))])))
