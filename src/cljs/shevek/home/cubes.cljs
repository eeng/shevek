(ns shevek.home.cubes
  (:require [reagent.core :as r]
            [shevek.reflow.core :refer [dispatch] :refer-macros [defevh]]
            [shevek.reflow.db :as db]
            [shevek.i18n :refer [t]]
            [shevek.lib.react :refer [rmap]]
            [shevek.lib.util :refer [trigger]]
            [shevek.lib.string :refer [present?]]
            [shevek.lib.dw.cubes :refer [fetch-cubes cubes-list]]
            [shevek.lib.time.ext :refer [format-time]]
            [shevek.components.form :refer [search-input filter-matching by]]
            [shevek.components.popup :refer [tooltip]]))

(defn- cube-card [{:keys [name title description min-time max-time]}]
  [:a.ui.fluid.cube.card {:on-click #(dispatch :cube-selected name)}
   [:div.content
    [:div.header [:i.cube.icon] title]
    [:div.description (if (present? description) description (t :errors/no-desc))]]
   [:div.extra.content
    [:span.right.floated {:ref (tooltip (t :cubes/data-range))} 
     (format-time min-time :day) " - " (format-time max-time :day)]]])

(defn cubes-cards []
  (fetch-cubes)
  (let [search (r/atom "")]
    (fn []
      (let [cubes (cubes-list)]
        [:div.column
         [:h2.ui.app.header (t :cubes/title)]
         [search-input search {:on-enter #(trigger "click" ".cube.card")}]
         (if (db/get :cubes)
           (let [cubes (filter-matching @search (by :title :description) cubes)]
             (if (seq cubes)
               [:div.cards (rmap cube-card :name cubes)]
               [:div.large.tip (if (seq @search) (t :errors/no-results) (t :cubes/missing))]))
           [:div.ui.active.inline.centered.loader])]))))
