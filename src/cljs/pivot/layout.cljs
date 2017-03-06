(ns pivot.layout)

(defn layout [page]
  [:div
   [:div.ui.fixed.inverted.menu
    [:div.ui.container
      [:a.item {:href "#"} "Dashboard"]
      [:a.item {:href "#"} "Cubos"]
      [:div.right.menu
       [:a.item {:href "#"} "Salir"]]]]
   [:div.ui.page.container
    [page]]])
