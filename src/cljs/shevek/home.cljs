(ns shevek.home
  (:require-macros [shevek.reflow.macros :refer [defevh]])
  (:require [reagent.core :as r]
            [cuerdas.core :as str]
            [shevek.reflow.core :refer [dispatch]]
            [shevek.reflow.db :as db]
            [shevek.i18n :refer [t translation!]]
            [shevek.rpc :as rpc]
            [shevek.components.text :refer [page-title]]
            [shevek.components.form :refer [search-input filter-matching by input-field kb-shortcuts hold-to-confirm]]
            [shevek.lib.dw.cubes :as c :refer [fetch-cubes]]
            [shevek.lib.react :refer [rmap]]
            [shevek.lib.dates :refer [format-time now]]
            [shevek.lib.string :refer [present?]]
            [shevek.lib.util :refer [new-record?]]
            [shevek.menu.reports :refer [fetch-reports save-report-form report-actions]]
            [shevek.notification :refer [notify]]))

(defn- cube-card [{:keys [name title description]}]
  [:a.ui.fluid.card {:on-click #(dispatch :cube-selected name)}
   [:div.content
    [:div.header [:i.cube.icon] title]
    [:div.description (if (present? description) description (t :errors/no-desc))]]])

(defn- report-card []
  (let [form-data (r/atom nil)]
    (fn [{:keys [name description updated-at cube] :as report}]
      (if @form-data
        [:div.ui.fluid.card
         [:div.content
          [save-report-form form-data]]]
        [:a.ui.fluid.card {:on-click #(dispatch :report-selected report)}
         [:div.content
          [:div.right.floated [report-actions report form-data]]
          [:div.header [:i.line.chart.icon] name]
          (when (present? description)
            [:div.description description])]
         [:div.extra.content
          [:i.cube.icon]
          (db/get-in [:cubes cube :title])
          [:span.right.floated (format-time updated-at :day)]]]))))

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

(defn- cards []
  (let [search (r/atom "")]
    (fn [key records filter card-fn & {:keys [action]}]
      [:div.column
       [:h2.ui.app.header (translation! key :title)]
       (when action [:div.actions action])
       [search-input search {:input {:auto-focus (= key :cubes)}}]
       (if records
         (let [records (filter-matching @search filter records)]
           (if (seq records)
             (rmap card-fn (comp hash (juxt :name :created-at)) records)
             [:div.large.tip (if (seq @search) (t :errors/no-results) (translation! key :missing))]))
         [:div.ui.active.inline.centered.loader])])))

(defn page []
  (fetch-cubes)
  (fetch-reports)
  (fetch-dashboards)
  (fn []
    [:div#home.ui.container
     [page-title (t :home/title) (t :home/subtitle) "home layout"]
     [:div.ui.equal.width.grid
      [cards :cubes (c/cubes-list) (by :title :description) cube-card]
      [cards :dashboards (db/get :dashboards) (by :name :description) dashboard-card
       :action [:button.ui.compact.button {:on-click #(dispatch :new-dashboard-started)} (t :actions/new)]]
      [cards :reports (db/get :reports) (by :name :description) report-card]]]))
