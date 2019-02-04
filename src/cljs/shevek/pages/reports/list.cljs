(ns shevek.pages.reports.list
  (:require [reagent.core :as r]
            [shevek.reflow.db :as db]
            [shevek.i18n :refer [t]]
            [shevek.reflow.core :refer [dispatch]]
            [shevek.components.form :refer [search-input filter-matching by]]
            [shevek.lib.react :refer [rmap]]))

(defn- report-item [{:keys [id name description]}]
  [:a.item {:href (str "/reports/" id) :tab-index -1}
   [:i.line.graph.icon]
   [:div.content
    [:div.header name]
    [:div description]]])

(defn reports-list []
  (dispatch :reports/fetch)
  (let [search (r/atom "")]
    (fn []
      (let [reports (db/get :reports)
            filtered-reports (filter-matching @search (by :name :description) reports)]
        (cond
          (not reports)
          [:div.ui.active.inline.centered.loader]

          (empty? reports)
          [:div.tip (t :reports/missing)]

          :else
          [:div
           [search-input search {:placeholder (t :dashboards/search-hint) :input {:auto-focus false}}]
           [:div.ui.selection.list
            (rmap report-item :id filtered-reports)]
           (when (empty? filtered-reports)
             [:div.tip (t :errors/no-results)])])))))
