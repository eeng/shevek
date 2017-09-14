(ns shevek.home.cubes
  (:require [reagent.core :as r]
            [shevek.reflow.core :refer [dispatch]]
            [shevek.reflow.db :as db]
            [shevek.i18n :refer [t]]
            [shevek.lib.react :refer [rmap]]
            [shevek.lib.util :refer [trigger]]
            [shevek.lib.string :refer [present?]]
            [shevek.lib.dw.cubes :refer [fetch-cubes cubes-list]]
            [shevek.components.form :refer [search-input filter-matching by]]))

(defn- cube-card [{:keys [name title description]}]
  [:a.ui.fluid.cube.card {:on-click #(dispatch :cube-selected name)}
   [:div.content
    [:div.header [:i.cube.icon] title]
    [:div.description (if (present? description) description (t :errors/no-desc))]]])

(defn cubes-cards []
  (fetch-cubes)
  (let [search (r/atom "")]
    (fn []
      (let [cubes (cubes-list)]
        [:div.column
         [:h2.ui.app.header (t :cubes/title)]
         [search-input search {:on-enter #(trigger "click" ".cube.card")}]
         (if cubes
           (let [cubes (filter-matching @search (by :title :description) cubes)]
             (if (seq cubes)
               (rmap cube-card :name cubes)
               [:div.large.tip (if (seq @search) (t :errors/no-results) (t :cubes/missing))]))
           [:div.ui.active.inline.centered.loader])]))))