(ns shevek.pages.reports.page
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

(defevh :reports/fetch [db]
  (rpc/fetch db :reports "reports/find-all"))

(defevh :reports/delete [db {:keys [id name]}]
  (rpc/call "reports/delete" :args [id] :handler #(do
                                                    (dispatch :reports/fetch)
                                                    (notify (t :reports/deleted name)))))

(defn- clicked-inside-actions? [node]
  (-> node .-target js/$ (.closest "td.actions") .-length (> 0)))

(defn- report-row [{:keys [id name description updated-at] :as report}]
  [:tr.selectable {:on-click #(when-not (clicked-inside-actions? %)
                                (navigate "/reports/" id))}
   [:td name]
   [:td description]
   [:td (format-time updated-at :day)]
   [:td.actions
    [with-confirm
     [:button.ui.inverted.compact.circular.red.icon.button
      {:on-click #(dispatch :reports/delete report)}
      [:i.trash.icon]]]]])

(defn- reports-table [search]
  (let [reports (filter-matching @search (by :name :description) (db/get :reports))]
    (if (seq reports)
      [:table.ui.striped.table
       [:thead>tr
        [:th (t :reports/name)]
        [:th (t :reports/description)]
        [:th (t :reports/updated-at)]
        [:th.center.aligned.collapsing (t :actions/header)]]
       [:tbody
        (for [{:keys [id] :as report} reports]
          ^{:key id} [report-row report])]]
      [:div.large.tip (t :errors/no-results)])))

(defn page []
  (dispatch :reports/fetch)
  (let [search (r/atom "")]
    (fn []
      [page-with-header
       {:title (t :reports/title) :subtitle (t :reports/subtitle) :icon "line graph layout"}
       (if (db/get :reports)
         [:div.ui.grid
          [:div.five.wide.column
           [search-input search {:placeholder (t :dashboards/search-hint)}]]
          [:div.sixteen.wide.column
           [reports-table search]]]
         [page-loader])])))
