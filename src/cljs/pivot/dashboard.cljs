(ns pivot.dashboard
  (:require [reagent.core :as r]))

(def cubes
  (r/atom [{:name "vtol_stats" :title "VTOL Stats" :description "Estadísticas de uso del sistema VTOL."}
           {:name "eventos_pedidos" :title "Eventos de Pedidos" :description "Info sobre eventos generados por el ruteo de pedidos."}]))

#_(def cubes (r/atom []))

(defonce colors ["blue" "orange" "green" "olive" "teal" "red"
                 "purple" "yellow" "violet" "brown" "pink" "grey"])

(defn page []
  [:div
   [:h1.ui.dividing.header "Cubos de Datos"]
   [:div.ui.cards
    (if (seq @cubes)
      (for [[i {:keys [name title description]}] (map-indexed vector @cubes)]
        ^{:key i}
        [:a.card {:href (str "/cubes/" name)}
         [:div.content
          [:div.ui.header
           [:i.cube.icon {:class (colors i)}]
           [:div.content title]]
          [:div.description description]]])
      [:div.ui.basic.segment "No hay cubos definidos."])]])
