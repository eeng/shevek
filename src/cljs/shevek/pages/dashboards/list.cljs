(ns shevek.pages.dashboards.list
  (:require [reagent.core :as r]
            [shevek.reflow.db :as db]
            [shevek.i18n :refer [t]]
            [shevek.reflow.core :refer [dispatch]]
            [shevek.components.form :refer [search-input filter-matching by]]
            [shevek.lib.react :refer [rmap]]))

(defn- dashboard-item [{:keys [id name description]}]
  [:a.item {:href (str "/dashboards/" id) :tab-index -1}
   [:i.block.layout.icon]
   [:div.content
    [:div.header name]
    [:div description]]])

(defn- create-button []
  [:a.ui.green.button {:href "/dashboards/new"}
   [:i.plus.icon] (t :actions/new)])

(defn dashboards-list []
  (dispatch :dashboards/fetch)
  (let [search (r/atom "")]
    (fn []
      (let [dashboards (db/get :dashboards)
            filtered-dashboards (filter-matching @search (by :name :description) dashboards)]
        [:<>
         (cond
           (not dashboards)
           [:div.ui.active.inline.centered.loader]

           (empty? dashboards)
           [:div.tip (t :dashboards/missing)]

           :else
           [:<>
            [search-input search {:placeholder (t :dashboards/search-hint) :input {:auto-focus false}}]
            (if (seq filtered-dashboards)
              [:div.ui.selection.list
               (rmap dashboard-item :id filtered-dashboards)]
              [:div.top.spaced.tip (t :errors/no-results)])])

         [:div.ui.divider]
         [create-button]
         (when (seq dashboards)
           [:a.ui.button {:href "/dashboards"}
            (t :actions/manage)])]))))
