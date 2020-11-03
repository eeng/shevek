(ns shevek.pages.dashboards.page
  (:require [reagent.core :as r]
            [shevek.reflow.core :refer [dispatch] :refer-macros [defevh]]
            [shevek.reflow.db :as db]
            [shevek.rpc :as rpc]
            [shevek.lib.time.ext :refer [format-time]]
            [shevek.navigation :refer [navigate]]
            [shevek.components.layout :refer [page-with-header page-loader]]
            [shevek.components.form :refer [search-input filter-matching by]]
            [shevek.components.confirmation :refer [with-confirm]]
            [shevek.components.notification :refer [notify]]
            [shevek.i18n :refer [t]]))

(defevh :dashboards/fetch [db]
  (rpc/fetch db :dashboards "dashboards/find-all"))

(defevh :dashboards/delete [_db {:keys [id name]}]
  (rpc/call "dashboards/delete" :args [id] :handler #(do
                                                       (dispatch :dashboards/fetch)
                                                       (notify (t :dashboards/deleted name)))))

(defn- clicked-inside-actions? [node]
  (-> node .-target js/$ (.closest "td.actions") .-length (> 0)))

(defn- dashboard-row [{:keys [id name description updated-at] :as dashboard}]
  [:tr.selectable {:on-click #(when-not (clicked-inside-actions? %)
                                (navigate "/dashboards/" id))}
   [:td name]
   [:td description]
   [:td.single.line (format-time updated-at :day)]
   [:td.actions
    [with-confirm
     [:button.ui.inverted.compact.circular.red.icon.button
      {:on-click #(dispatch :dashboards/delete dashboard)}
      [:i.trash.icon]]]]])

(defn- dashboards-table [search]
  (let [dashboards (filter-matching @search (by :name :description) (db/get :dashboards))]
    (if (seq dashboards)
      [:table.ui.striped.table
       [:thead>tr
        [:th (t :dashboards/name)]
        [:th (t :dashboards/description)]
        [:th (t :dashboards/updated-at)]
        [:th.center.aligned.collapsing (t :actions/header)]]
       [:tbody
        (for [{:keys [id] :as dashboard} dashboards]
          ^{:key id} [dashboard-row dashboard])]]
      [:div.large.tip (t :errors/no-results)])))

(defn page []
  (dispatch :dashboards/fetch)
  (let [search (r/atom "")]
    (fn []
      [page-with-header
       {:title (t :dashboards/title) :subtitle (t :dashboards/subtitle) :icon "block layout"}
       (if (db/get :dashboards)
         [:div.ui.grid
          [:div.five.wide.column
           [search-input search {:placeholder (t :dashboards/search-hint)}]]
          [:div.eleven.wide.column.right.aligned
           [:a.ui.green.button {:href "/dashboards/new"}
            [:i.plus.icon] (t :actions/new)]]
          [:div.sixteen.wide.column
           [dashboards-table search]]]
         [page-loader])])))
