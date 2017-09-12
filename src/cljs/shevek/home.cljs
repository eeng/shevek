(ns shevek.home
  (:require-macros [shevek.reflow.macros :refer [defevh]])
  (:require [reagent.core :as r]
            [cuerdas.core :as str]
            [shevek.reflow.core :refer [dispatch]]
            [shevek.reflow.db :as db]
            [shevek.i18n :refer [t translation!]]
            [shevek.rpc :as rpc]
            [shevek.components.text :refer [page-title]]
            [shevek.components.form :refer [search-input filter-matching by]]
            [shevek.lib.dw.cubes :as c :refer [fetch-cubes]]
            [shevek.lib.react :refer [rmap]]
            [shevek.lib.dates :refer [format-time]]
            [shevek.lib.string :refer [present?]]
            [shevek.menu.reports :refer [fetch-reports save-report-form report-actions]]))

(defevh :dashboards-requested [db]
  (rpc/fetch db :dashboards "dashboards.api/find-all"))

(defn fetch-dashboards []
  (dispatch :dashboards-requested))

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

(defn- dashboard-card [{:keys [name description updated-at]}]
  [:a.ui.fluid.card {:on-click #(dispatch :dashboard-selected name)}
   [:div.content
    [:div.header [:i.block.icon] name]
    (when (present? description)
      [:div.description description])
    [:div.extra.content
     [:span.right.floated (format-time updated-at :day)]]]])

(defn- cards []
  (let [search (r/atom "")]
    (fn [key records filter card-fn]
      [:div.column
       [:h2.ui.app.header (translation! key :title)]
       [search-input search {:input {:auto-focus (= key :cubes)}}]
       (if records
         (let [records (filter-matching @search filter records)]
           (if (seq records)
             (rmap card-fn :name records)
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
      [cards :dashboards (db/get :dashboards) (by :name :description) dashboard-card]
      [cards :reports (db/get :reports) (by :name :description) report-card]]]))
