(ns pivot.dashboard
  (:require [reflow.core :refer [dispatch]]
            [reflow.db :as db]
            [pivot.i18n :refer [t]]
            [pivot.components :refer [page-title]]
            [pivot.dw :as dw]))

(defn- cube-card [i {:keys [name title description] :or {description (t :cubes/no-desc)}}]
  ^{:key i}
  [:a.card {:href (str "#/cubes/" name)}
   [:div.content
    [:div.ui.header
     [:i.cube.blue.icon]
     [:div.content title]]
    [:div.description description]]])

(defn- cubes-cards []
  (if (db/get :cubes)
    [:div.ui.cards
     (if (seq (db/get :cubes))
       (doall (map-indexed cube-card (db/get :cubes)))
       [:div.ui.basic.segment (t :cubes/missing)])]
    [:div.ui.active.inline.loader]))

(defn page []
  (dw/fetch-cubes)
  (fn []
    [:div.ui.container
     [page-title (t :dashboard/title) (t :dashboard/subtitle) "block layout"]
     [:h2.ui.dividing.header (t :cubes/title)]
     [cubes-cards]]))
