(ns pivot.dashboard
  (:require [reagent.core :as r]
            [pivot.i18n :refer [t]]
            [pivot.rpc :as rpc]))

(defonce colors ["blue" "orange" "green" "olive" "teal" "red"
                 "purple" "yellow" "violet" "brown" "pink" "grey"])

(defonce cubes (r/atom nil))

(defn load-cubes []
  (rpc/call "handler/get-cubes" :handler #(reset! cubes %)))

(defn- cube-card [i {:keys [name title description]}]
  ^{:key i}
  [:a.card {:href (str "/cubes/" name)}
   [:div.content
    [:div.ui.header
     [:i.cube.icon {:class (colors i)}]
     [:div.content title]]
    [:div.description description]]])

(defn page []
  (load-cubes)
  (fn []
    [:div
     [:h1.ui.dividing.header (t :cubes/title)]
     [:div.ui.cards
      (if @cubes
        (map-indexed cube-card @cubes)
        [:div.ui.basic.segment (t :cubes/missing)])]]))
