(ns shevek.home.dashboards
  (:require-macros [shevek.reflow.macros :refer [defevh]])
  (:require [reagent.core :as r]
            [shevek.reflow.core :refer [dispatch]]
            [shevek.reflow.db :as db]
            [shevek.i18n :refer [t]]
            [shevek.rpc :as rpc]
            [shevek.components.form :refer [search-input filter-matching by input-field kb-shortcuts hold-to-confirm]]
            [shevek.lib.react :refer [rmap]]
            [shevek.lib.dates :refer [format-time]]
            [shevek.lib.string :refer [present?]]
            [shevek.lib.util :refer [new-record?]]
            [shevek.notification :refer [notify]]))

(defn fetch-dashboards []
  (dispatch :dashboards-requested))

(defevh :dashboards-requested [db]
  (rpc/fetch db :dashboards "dashboards.api/find-all"))

(defevh :new-dashboard-started [{:keys [dashboards] :as db}]
  (update db :dashboards conj {:name (str "Dashboard #" (inc (count dashboards)))}))

(defevh :dashbard-edition-canceled [{:keys [dashboards] :as db} dashboard]
  (update db :dashboards (partial remove #{dashboard})))

(defevh :dashboard-saved [db]
  (fetch-dashboards)
  (rpc/loaded db :saving-dashboard))

(defevh :dashboard-edition-saved [db dashboard]
  (rpc/call "dashboards.api/save" :args [dashboard] :handler #(dispatch :dashboard-saved))
  (rpc/loading db :saving-dashboard))

(defn- dashboard-card [dashboard]
  (let [form-data (r/atom (when (new-record? dashboard) (dissoc dashboard :name)))
        cancel (fn [_]
                 (dispatch :dashbard-edition-canceled dashboard)
                 (reset! form-data nil))
        save (fn [_]
               (dispatch :dashboard-edition-saved @form-data)
               (reset! form-data nil))
        shortcuts (kb-shortcuts :enter save :escape cancel)]
    (fn [{:keys [name description updated-at] :as dashboard}]
      (let [valid? (present? (:name @form-data))
            delete (fn []
                     (rpc/call "dashboards.api/delete" :args [dashboard]
                               :handler #(do (notify (t :dashboards/deleted name))
                                           (dispatch :dashbard-edition-canceled dashboard))))]
        (if @form-data
          [:div.ui.fluid.card
           [:div.content
            [:div.ui.form {:ref shortcuts}
             [input-field form-data :name {:label (t :reports/name) :class "required" :auto-focus true :placeholder (dashboard :name)}]
             [input-field form-data :description {:label (t :reports/description) :as :textarea :rows 2}]
             [:button.ui.primary.button {:on-click save :class (when-not valid? "disabled")} (t :actions/save)]
             [:button.ui.button {:on-click cancel} (t :actions/cancel)]]]]
          [:a.ui.fluid.card {:on-click #(dispatch :dashboard-selected dashboard)}
           [:div.content
            [:div.right.floated
             [:div.item-actions
              [:i.trash.icon (hold-to-confirm delete)]]]
            [:div.header [:i.block.layout.icon] name]
            (when (present? description)
              [:div.description description])]
           [:div.extra.content
            [:span.right.floated (format-time updated-at :day)]]])))))

(defn dashboards-cards []
  (fetch-dashboards)
  (let [search (r/atom "")]
    (fn []
      (let [dashboards (db/get :dashboards)]
        [:div.column
         [:h2.ui.app.header (t :dashboards/title)]
         [:div.actions
          [:button.ui.compact.button {:on-click #(dispatch :new-dashboard-started)}
           (t :actions/new)]]
         [search-input search {:input {:auto-focus false}}]
         (if dashboards
           (let [dashboards (filter-matching @search (by :name :description) dashboards)]
             (if (seq dashboards)
               (rmap dashboard-card (comp hash (juxt :name :created-at)) dashboards)
               [:div.large.tip (if (seq @search) (t :errors/no-results) (t :dashboards/missing))]))
           [:div.ui.active.inline.centered.loader])]))))
