(ns pivot.layout
  (:require [pivot.i18n :refer [t]]))

(defn layout [page]
  [:div
   [:div.ui.fixed.inverted.menu
    [:div.ui.container
      [:a.item {:href "#"} "Dashboard"]
      [:a.item {:href "#"} (t :cubes/menu)]
      [:div.right.menu
       [:a.item {:href "#"} (t :menu/logout)]]]]
   [:div.ui.page.container
    [page]]])
