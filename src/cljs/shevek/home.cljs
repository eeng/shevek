(ns shevek.home
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
            [shevek.menu.reports :refer [fetch-reports]]))

(defn- cube-card [{:keys [name title description]}]
  [:a.ui.fluid.card {:on-click #(dispatch :cube-selected name)}
   [:div.content
    [:div.header [:i.cube.icon] title]
    [:div.meta (if (str/blank? description) (t :errors/no-desc) description)]]])

(defn- report-card [{:keys [name description updated-at] :as report}]
  [:a.ui.fluid.card {:on-click #(dispatch :report-selected report)}
   [:div.content
    [:div.header [:i.line.chart.icon] name]
    [:div.meta
     [:p description]
     [:p (t :reports/updated-at) ": " (format-time updated-at)]]]])

(defn- dashboard-card [{:keys [name description]}]
  [:a.ui.fluid.card {:on-click #(dispatch :dashboard-selected name)}
   [:div.content
    [:div.header [:i.block.icon] name]
    [:div.meta (if (str/blank? description) (t :errors/no-desc) description)]]])

(defn- cards []
  (let [search (r/atom "")]
    (fn [key records filter card-fn]
      [:div.column
       [:h2.ui.app.header (translation! key :title)]
       [search-input search {:input {:auto-focus false} :wrapper {:class "big"}}]
       (if records
         (let [records (filter-matching @search filter records)]
           (if (seq records)
             (rmap card-fn :name records)
             [:div.large.tip (if (seq @search) (t :errors/no-results) (translation! key :missing))]))
         [:div.ui.active.inline.centered.loader])])))

(defn page []
  (fetch-cubes)
  (fetch-reports)
  (fn []
    [:div#home.ui.container
     [page-title (t :home/title) (t :home/subtitle) "home layout"]
     [:div.ui.equal.width.grid
      [cards :cubes (c/cubes-list) (by :title :description) cube-card]
      [cards :dashboards (db/get :dashboards) (by :name :description) dashboard-card]
      [cards :reports (db/get :reports) (by :name :description) report-card]]]))
