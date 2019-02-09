(ns shevek.pages.cubes.list
  (:require [reagent.core :as r]
            [shevek.i18n :refer [t]]
            [shevek.reflow.core :refer [dispatch]]
            [shevek.reflow.db :as db]
            [shevek.pages.cubes.page :as p]
            [shevek.components.form :refer [search-input filter-matching by]]))

(defn fetch-cubes []
  (when-not (db/get :cubes)
    (dispatch :cubes/fetch)))

(defn- cube-item [{:keys [name title description]} {:keys [on-click]}]
  [:a.item {:href (when-not on-click (str "/reports/new/" name))
            :on-click (when on-click #(on-click name))
            :tab-index -1}
   [:i.cube.icon]
   [:div.content
    [:div.header title]
    [:div description]]])

(defn cubes-list [opts]
  (fetch-cubes)
  (let [search (r/atom "")]
    (fn [opts]
      (let [cubes (p/cubes-list)
            filtered-cubes (filter-matching @search (by :title :description) cubes)]
        (cond
          (not cubes)
          [:div.ui.active.inline.centered.loader]

          (empty? cubes)
          [:div.tip (t :cubes/missing)]

          :else
          [:<>
           [search-input search {:placeholder (t :dashboards/search-hint)}]
           [:div.ui.selection.list
            (for [{:keys [name] :as cube} filtered-cubes]
              ^{:key name} [cube-item cube opts])]
           (when (empty? filtered-cubes)
             [:div.tip (t :errors/no-results)])])))))
