(ns shevek.home.dashboards
  (:require [reagent.core :as r]
            [shevek.reflow.core :refer [dispatch] :refer-macros [defevh]]
            [shevek.reflow.db :as db]
            [shevek.i18n :refer [t]]
            [shevek.rpc :as rpc]
            [shevek.components.form :refer [search-input filter-matching by input-field kb-shortcuts hold-to-confirm]]
            [shevek.lib.react :refer [rmap]]
            [shevek.lib.time :refer [now]]
            [shevek.lib.time.ext :refer [format-time]]
            [shevek.lib.string :refer [present?]]
            [shevek.lib.util :refer [new-record? trigger]]
            [shevek.notification :refer [notify]]
            [shevek.navigation :refer [navigate]]))

(defn fetch-dashboards []
  (dispatch :dashboards-requested))

(defevh :dashboards-requested [db]
  (rpc/fetch db :dashboards "dashboards/find-all"))

(defevh :new-dashboard-started [{:keys [dashboards] :as db}]
  (update db :dashboards conj {:name "" :created-at (now) :updated-at (now)}))

(defevh :dashbard-edition-canceled [{:keys [dashboards] :as db} dashboard]
  (cond-> db
          (new-record? dashboard) (update :dashboards (partial remove #{dashboard}))))

(defevh :dashboard-edit-saved [db form-data]
  (let [dashboard (dissoc @form-data :created-at :updated-at)]
    (rpc/call "dashboards/save" :args [dashboard] :handler #(dispatch :dashboard-saved % form-data))
    (rpc/loading db :saving-dashboard)))

(defevh :dashboard-saved [db {:keys [name]} form-data]
  (fetch-dashboards)
  (notify (t :dashboards/saved name))
  (reset! form-data nil)
  (rpc/loaded db :saving-dashboard))

(defevh :dashboard-deleted [db {:keys [name] :as dashboard}]
  (rpc/call "dashboards/delete" :args [dashboard] :handler #(notify (t :dashboards/deleted name)))
  (update db :dashboards (partial remove #{dashboard})))

(defn select-dashboard [{:keys [id]}]
  (navigate "/dashboard/" id))

(defn- show-card [{:keys [name description updated-at reports] :as dashboard} form-data]
  [:a.ui.fluid.dashboard.card {:on-click #(select-dashboard dashboard)}
   [:div.content
    [:div.right.floated
     [:div.item-actions {:on-click #(.stopPropagation %)}
      [:i.write.icon {:on-click #(reset! form-data dashboard)}]
      [:i.trash.icon (hold-to-confirm #(dispatch :dashboard-deleted dashboard))]]]
    [:div.header [:i.block.layout.icon] name]
    (when (present? description)
      [:div.description description])]
   [:div.extra.content
    [:i.line.chart.icon]
    (t :dashboards/report-count (count reports))
    [:span.right.floated (format-time updated-at :day)]]])

(defn- form-card [{:keys [name] :as dashboard} form-data]
  (let [cancel (fn [_]
                 (dispatch :dashbard-edition-canceled dashboard)
                 (reset! form-data nil))
        valid? #(present? (:name @form-data))
        save #(when (valid?) (dispatch :dashboard-edit-saved form-data))
        shortcuts (kb-shortcuts :enter save :escape cancel)]
    (fn []
      [:div.ui.fluid.card
       [:div.content
        [:div.ui.form (assoc (rpc/loading-class :saving-dashboard) :ref shortcuts)
         [input-field form-data :name {:label (t :reports/name) :class "required" :auto-focus true}]
         [input-field form-data :description {:label (t :reports/description) :as :textarea :rows 2}]
         [:button.ui.primary.button {:on-click save :class (when-not (valid?) "disabled")} (t :actions/save)]
         [:button.ui.button {:on-click cancel} (t :actions/cancel)]]]])))

(defn- dashboard-card [dashboard]
  (let [form-data (r/atom (when (new-record? dashboard)
                            (dissoc dashboard :name)))]
    (fn [dashboard]
      (if @form-data
        [form-card dashboard form-data]
        [show-card dashboard form-data]))))

(defn dashboards-cards []
  (fetch-dashboards)
  (let [search (r/atom "")]
    (fn []
      (let [dashboards (db/get :dashboards)]
        [:div.column
         [:h2.ui.app.header (t :dashboards/title)]
         [:div.actions
          [:button.ui.compact.button {:on-click #(dispatch :new-dashboard-started) :tab-index -1}
           (t :actions/new)]]
         [search-input search {:on-enter #(trigger "click" ".dashboard.card") :input {:auto-focus false}}]
         (if dashboards
           (let [dashboards (filter-matching @search (by :name :description) dashboards)]
             (if (seq dashboards)
               [:div.cards (rmap dashboard-card (comp hash (juxt :name :created-at)) dashboards)]
               [:div.large.tip (if (seq @search) (t :errors/no-results) (t :dashboards/missing))]))
           [:div.ui.active.inline.centered.loader])]))))
