(ns pivot.layout
  (:require [pivot.i18n :refer [t]]))

(defn layout [page]
  [:div
   [:div.ui.fixed.inverted.menu
    [:div.ui.container
      [:a.item {:href "#"} [:i.block.layout.icon] (t :dashboard/menu)]
      [:a.item {:href "#"} [:i.cubes.icon] (t :cubes/menu)]
      [:div.right.menu
       [:a.item {:href "#"} [:i.sign.out.icon] (t :menu/logout)]]]]
   [:div.ui.page.container
    [page]]])
