(ns shevek.pages.cubes.list
  (:require [reagent.core :as r]
            [shevek.i18n :refer [t]]
            [shevek.reflow.core :refer [dispatch]]
            [shevek.pages.cubes.page :as p]
            [shevek.components.form :refer [search-input filter-matching by]]
            [shevek.lib.react :refer [rmap]]))

(defn- cube-item [{:keys [name title description]}]
  [:a.item {:href (str "/reports/new/" name) :tab-index -1}
   [:i.cube.icon]
   [:div.content
    [:div.header title]
    [:div description]]])

(defn cubes-list []
  (dispatch :cubes/fetch)
  (let [search (r/atom "")]
    (fn []
      (let [cubes (p/cubes-list)
            filtered-cubes (filter-matching @search (by :title :description) cubes)]
        (cond
          (not cubes)
          [:div.ui.active.inline.centered.loader]

          (empty? cubes)
          [:div.tip (t :cubes/missing)]

          :else
          [:div
           [search-input search {:placeholder (t :dashboards/search-hint)}]
           [:div.ui.selection.list
            (rmap cube-item :name filtered-cubes)]
           (when (empty? filtered-cubes)
             [:div.tip (t :errors/no-results)])])))))
