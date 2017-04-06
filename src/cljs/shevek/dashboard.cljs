(ns shevek.dashboard
  (:require [reflow.core :refer [dispatch]]
            [reflow.db :as db]
            [shevek.i18n :refer [t]]
            [shevek.rpc :as rpc]
            [shevek.components :refer [page-title]]
            [shevek.dw :as dw]))

(defn- cube-card [i {:keys [name title description] :or {description (t :cubes/no-desc)}}]
  ^{:key i}
  [:a.card {:href (str "#/cubes/" name)}
   [:div.content
    [:div.ui.header
     [:i.cube.blue.icon]
     [:div.content title]]
    [:div.description description]]])

(defn- cubes-cards []
  (let [cubes (dw/cubes-list)]
    [:div.ui.cards
     (if (seq cubes)
       (doall (map-indexed cube-card cubes))
       [:div.ui.basic.segment (t :cubes/missing)])]))

(defn page []
  (dw/fetch-cubes)
  (fn []
    [:div.ui.container
     [page-title (t :dashboard/title) (t :dashboard/subtitle) "block layout"]
     [:h2.ui.dividing.header (t :cubes/title)]
     [cubes-cards]]))
