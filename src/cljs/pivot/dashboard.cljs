(ns pivot.dashboard
  (:require [reflow.core :refer [dispatch]]
            [reflow.db :as db]
            [pivot.i18n :refer [t]]
            [pivot.components :refer [page-title]]
            [pivot.rpc]))

(defn- cube-card [i {:keys [name title description]}]
  ^{:key i}
  [:a.card {:href (str "/cubes/" name)}
   [:div.content
    [:div.ui.header
     [:i.cube.blue.icon]
     [:div.content title]]
    [:div.description description]]])

(defn- cubes-cards []
  (if (db/get :cubes)
    [:div.ui.cards
     (if (seq (db/get :cubes))
       (map-indexed cube-card (db/get :cubes))
       [:div.ui.basic.segment (t :cubes/missing)])]
    [:div.ui.active.inline.loader]))

(defn page []
  (dispatch :data-requested :cubes "handler/get-cubes")
  (fn []
    [:div.ui.container
     [page-title (t :dashboard/title) (t :dashboard/subtitle) "block layout"]
     [:h2.ui.dividing.header (t :cubes/title)]
     [cubes-cards]]))
