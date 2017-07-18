(ns shevek.menu.share
  (:require [shevek.components.popup :refer [show-popup]]))

(defn- popup-content []
  [:div "TODO"])

(defn share-menu []
  [:a.icon.item {:on-click #(show-popup % popup-content {:position "bottom center"})}
   [:i.share.alternate.icon]])
