(ns shevek.pages.cubes.page
  (:require [reagent.core :as r]
            [shevek.reflow.db :as db]
            [shevek.navigation :refer [navigate]]
            [shevek.components.layout :refer [page-with-header page-loader]]
            [shevek.components.form :refer [search-input filter-matching by]]
            [shevek.pages.cubes.helpers :refer [cubes-list fetch-cubes]]
            [shevek.i18n :refer [t]]
            [shevek.lib.time.ext :refer [format-time]]))

(defn- cube-row [{:keys [name title description min-time max-time]}]
  [:tr.selectable {:on-click #(navigate "/reports/new/" name)}
   [:td title]
   [:td description]
   [:td (format-time min-time :day) " - " (format-time max-time :day)]])

(defn- cubes-table [search]
  (let [cubes (filter-matching @search (by :title :description) (cubes-list))]
    (if (seq cubes)
      [:table.ui.striped.table
       [:thead>tr
        [:th (t :cubes/name)]
        [:th (t :cubes/description)]
        [:th (t :cubes/data-range)]]
       [:tbody
        (for [{:keys [id] :as cube} cubes]
          ^{:key id} [cube-row cube])]]
      [:div.large.tip (t :errors/no-results)])))

(defn page []
  (fetch-cubes)
  (let [search (r/atom "")]
    (fn []
      [page-with-header
       {:title (t :cubes/title) :subtitle (t :cubes/subtitle) :icon "cubes"}
       (if (db/get :cubes)
         [:div.ui.grid
          [:div.five.wide.column
           [search-input search {:placeholder (t :dashboards/search-hint)}]]
          [:div.sixteen.wide.column
           [cubes-table search]]]
         [page-loader])])))
