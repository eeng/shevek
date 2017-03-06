(ns pivot.dashboard
  (:require [reagent.core :as r]))

(def cubes
  (r/atom [{:name "vtol_stats" :title "VTOL Stats" :description "Estadísticas de uso del sistema VTOL."}
           {:name "eventos_pedidos" :title "Eventos de Pedidos" :description "Info sobre eventos generados por el ruteo de pedidos."}]))

#_(def cubes (r/atom []))

(defn page []
  [:div
   [:h1.ui.dividing.header "Cubos de Datos"]
   [:div.ui.cards
    (if (seq @cubes)
      (for [{:keys [name title description]} @cubes]
        ^{:key name}
        [:a.card {:href (str "/cubes/" name)}
         [:div.content
          [:div.ui.header
           [:i.cubes.blue.icon]
           [:div.content title]]
          [:div.description description]]])
      [:div.ui.basic.segment "No hay cubos definidos."])]])
