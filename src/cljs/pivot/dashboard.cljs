(ns pivot.dashboard
  (:require-macros [reflow.macros :refer [defevh]])
  (:require [reagent.core :as r]
            [reflow.core :refer [dispatch]]
            [reflow.db :as db]
            [pivot.i18n :refer [t]]
            [pivot.rpc :as rpc]))

(defonce colors ["blue" "orange" "green" "olive" "teal" "red"
                 "purple" "yellow" "violet" "brown" "pink" "grey"])

(defevh :load-cubes [db]
  (rpc/call "handler/get-cubes" :handler #(dispatch :cubes-arrived %))
  (assoc db :loading? true))

(defevh :cubes-arrived [db cubes]
  (assoc db :loading? false :cubes cubes))

(defn- cube-card [i {:keys [name title description]}]
  ^{:key i}
  [:a.card {:href (str "/cubes/" name)}
   [:div.content
    [:div.ui.header
     [:i.cube.icon {:class (colors i)}]
     [:div.content title]]
    [:div.description description]]])

(defn page []
  (dispatch :load-cubes)
  (fn []
    [:div
     [:h1.ui.dividing.header (t :cubes/title)]
     (if (db/get :cubes)
       [:div.ui.cards
        (if (seq (db/get :cubes))
          (map-indexed cube-card (db/get :cubes))
          [:div.ui.basic.segment (t :cubes/missing)])]
       [:div.ui.active.inline.loader])]))
