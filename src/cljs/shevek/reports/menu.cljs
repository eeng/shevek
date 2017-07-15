(ns shevek.reports.menu
  (:require-macros [shevek.reflow.macros :refer [defevh]])
  (:require [reagent.core :as r]
            [shevek.reflow.db :as db]
            [shevek.reflow.core :refer [dispatch]]
            [shevek.rpc :as rpc]
            [shevek.i18n :refer [t]]
            [shevek.lib.react :refer [without-propagation]]
            [shevek.components.popup :refer [show-popup close-popup]]
            [shevek.components.form :refer [kb-shortcuts input-field]]
            [shevek.navegation :refer [current-page? navigate]]
            [shevek.notification :refer [notify]]
            [shevek.schemas.conversion :refer [viewer->report]]
            [shevek.viewer.page :as viewer]
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

(defevh :save-report [db report]
  (let [editing-current? (or (nil? (:_id report))
                             (and (= (:_id report) (get-in db [:current-report :_id]))
                                  (current-page? :viewer)))
        report (if editing-current?
                 (merge report (viewer->report (db :viewer)))
                 report)]
    (rpc/call "reports.api/save-report" :args [report] :handler #(dispatch :report-saved % editing-current?))
    (rpc/loading db :save-report)))

(defevh :delete-report [db report]
  ; TODO unificar estas dos lineas ya que siempre que hay un call debe haber un loading
  (rpc/call "reports.api/delete-report" :args [report] :handler fetch-reports)
  (rpc/loading db :save-report))

(defn- save-report-form [form-data]
  (let [report (r/cursor form-data [:report])
        valid? #(seq (:name @report))
        cancel #(reset! form-data nil)
        save #(when (valid?)
                (dispatch :save-report @report)
                (when-not (:editing? @form-data) (close-popup))
                (cancel))
        shortcuts (kb-shortcuts :enter save :escape cancel)]
    (fn []
      [:div.ui.form {:ref shortcuts}
       [input-field report :name {:label (t :reports/name) :class "required" :auto-focus true}]
       [input-field report :description {:label (t :reports/description) :as :textarea :rows 2}]
       [input-field report :pin-in-dashboard {:label (t :reports/pin-in-dashboard) :as :checkbox :input-class "toggle"}]
       [:button.ui.primary.button {:on-click save :class (when-not (valid?) "disabled")} (t :actions/save)]
       [:button.ui.button {:on-click cancel} (t :actions/cancel)]])))

(defn- reports-list [form-data current-report]
  (let [reports (db/get :reports)
        show-actions? (current-page? :viewer)
        save #(if (:_id current-report)
                (do (dispatch :save-report current-report) (close-popup))
                (reset! form-data {:report current-report :editing? false}))
        save-as #(reset! form-data {:report (dissoc current-report :_id :name) :editing? false})
        edit #(reset! form-data {:report % :editing? true})
        select-report #(do (dispatch :report-selected %) (close-popup))
        cubes (db/get :cubes)]
    [:div
     (when show-actions?
       [:div.actions
        [:button.ui.compact.green.button {:on-click save} (t :actions/save)]
        [:button.ui.basic.compact.button {:on-click save-as} (t :actions/save-as)]])
     [:h3.ui.sub.orange.header {:class (when show-actions? "has-actions")} (t :reports/title)]
     (if (seq reports)
       [:div.ui.relaxed.middle.aligned.selection.list
        (for [{:keys [_id name description cube] :as report} reports]
          [:div.item {:key _id :on-click #(select-report report)}
           [:div.right.floated.content
            [:div.cube (:title (cubes cube))]
            [:div.item-actions
             [:i.write.icon {:on-click (without-propagation edit report)}]
             [:i.trash.icon {:on-click (without-propagation dispatch :delete-report report)}]]]
           [:div.header name]
           [:div.description description]])]
       [:div (t :cubes/no-results)])]))

(defn- popup-content []
  (fetch-reports)
  (let [form-data (r/atom nil)
        current-report (or (db/get :current-report) {:pin-in-dashboard false})]
    (fn []
      [:div#reports-popup
       (if @form-data
         [save-report-form form-data]
         [reports-list form-data current-report])])))

(defn- reports-menu []
  (let [report-name (str/prune (db/get-in [:current-report :name]) 30)]
    [:a.item {:on-click #(show-popup % popup-content {:position "bottom left"})}
     [:i.line.chart.icon] (or (and (current-page? :viewer) report-name) (t :reports/menu))]))
