(ns pivot.dashboard
  (:require [reflow.core :refer [dispatch]]
            [reflow.db :as db]
            [pivot.i18n :refer [t]]
            [pivot.rpc]))

(defn- cube-card [i {:keys [name title description]}]
  ^{:key i}
  [:a.card {:href (str "/cubes/" name)}
   [:div.content
    [:div.ui.header
     [:i.cube.blue.icon]
     [:div.content title]]
    [:div.description description]]])

(defn page []
  (dispatch :load-data :cubes "handler/get-cubes")
  (fn []
    [:div.ui.container
     [:h1.ui.dividing.header (t :cubes/title)]
     (if (db/get :cubes)
       [:div.ui.cards
        (if (seq (db/get :cubes))
          (map-indexed cube-card (db/get :cubes))
          [:div.ui.basic.segment (t :cubes/missing)])]
       [:div.ui.active.inline.loader])]))
