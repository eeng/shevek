(ns shevek.menu.reports
  (:require-macros [shevek.reflow.macros :refer [defevh]])
  (:require [reagent.core :as r]
            [shevek.reflow.db :as db]
            [shevek.reflow.core :refer [dispatch]]
            [shevek.rpc :as rpc]
            [shevek.i18n :refer [t]]
            [shevek.lib.react :refer [without-propagation]]
            [shevek.components.popup :refer [show-popup close-popup]]
            [shevek.components.form :refer [kb-shortcuts input-field hold-to-confirm]]
            [shevek.navegation :refer [current-page? navigate]]
            [shevek.notification :refer [notify]]
            [shevek.schemas.conversion :refer [viewer->report]]
            [shevek.viewer.page :as viewer]
            [shevek.lib.util :refer [new-record?]]
            [cuerdas.core :as str]))

; TODO Muy parecido a lo de users, de nuevo el patron de call, loading y loaded
(defevh :reports-arrived [db reports]
  (-> (assoc db :reports reports)
      (rpc/loaded :reports)))

(defevh :reports-requested [db]
  (rpc/call "reports.api/find-all" :handler #(dispatch :reports-arrived %))
  (rpc/loading db :reports))

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
  (= (:_id report) (get-in db [:current-report :_id])))

(defevh :save-report [db report]
  (let [editing-current? (or (new-record? report)
                             (and (current-report? db report) (current-page? :viewer)))
        report (if editing-current?
                 (merge report (viewer->report (db :viewer)))
                 report)]
    (rpc/call "reports.api/save-report" :args [report] :handler #(dispatch :report-saved % editing-current?))
    (rpc/loading db :save-report)))

(defevh :delete-report [db report]
  (rpc/call "reports.api/delete-report" :args [report] :handler fetch-reports)
  (cond-> (rpc/loading db :save-report)
          (current-report? db report) (dissoc :current-report)))

(defn- save-report-form [form-data]
  (let [report (r/cursor form-data [:report])
        valid? #(seq (:name @report))
        cancel #(reset! form-data nil)
        save #(when (valid?)
                (dispatch :save-report @report)
                (when-not (:editing? @form-data) (close-popup))
                (js/setTimeout cancel 300))
        shortcuts (kb-shortcuts :enter save :escape cancel)]
    (fn []
      [:div.ui.form {:ref shortcuts}
       [input-field report :name {:label (t :reports/name) :class "required" :auto-focus true}]
       [input-field report :description {:label (t :reports/description) :as :textarea :rows 2}]
       [input-field report :pin-in-dashboard {:label (t :reports/pin-in-dashboard) :as :checkbox :input-class "toggle"}]
       [:button.ui.primary.button {:on-click save :class (when-not (valid?) "disabled")} (t :actions/save)]
       [:button.ui.button {:on-click cancel} (t :actions/cancel)]])))

(defn- report-item [_ form-data]
  (let [holding (r/atom nil)
        select-report #(do (dispatch :report-selected %) (close-popup))
        cubes (db/get :cubes)
        edit #(reset! form-data {:report % :editing? true})]
    (fn [{:keys [_id name description cube] :as report} _]
      [:div.item {:on-click #(select-report report)}
       [:div.right.floated.content
        [:div.cube (:title (cubes cube))]
        [:div.item-actions
         [:i.write.icon {:on-click (without-propagation edit report) :title (t :actions/edit)}]
         [:i.trash.icon (hold-to-confirm holding #(dispatch :delete-report report))]]]
       [:div.header name]
       [:div.description description]])))

(defn- reports-list [form-data]
  (let [current-report (or (db/get :current-report) {:pin-in-dashboard false})
        reports (db/get :reports)
        show-actions? (current-page? :viewer)
        save #(if (new-record? current-report)
                (reset! form-data {:report current-report :editing? false})
                (do (dispatch :save-report current-report) (close-popup)))
        save-as #(reset! form-data {:report (dissoc current-report :_id :name) :editing? false})]
    [:div
     (when show-actions?
       [:div.actions
        [:button.ui.compact.green.button {:on-click save} (t :actions/save)]
        [:button.ui.basic.compact.button {:on-click save-as} (t :actions/save-as)]])
     [:h3.ui.sub.orange.header {:class (when show-actions? "has-actions")} (t :reports/title)]
     (if (seq reports)
       [:div.ui.relaxed.middle.aligned.selection.list
        (for [report reports]
          ^{:key (:_id report)} [report-item report form-data])]
       [:div (t :cubes/no-results)])]))

(defn- popup-content []
  (fetch-reports)
  (let [form-data (r/atom nil)]
    (fn []
      [:div#reports-popup
       (if @form-data
         [save-report-form form-data]
         [reports-list form-data])])))

(defn- reports-menu []
  (let [report-name (str/prune (db/get-in [:current-report :name]) 30)]
    [:a.item {:on-click #(show-popup % popup-content {:position "bottom left"})}
     [:i.line.chart.icon] (or (and (current-page? :viewer) report-name) (t :reports/menu))]))
