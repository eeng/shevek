(ns shevek.menu.share
  (:require [shevek.i18n :refer [t]]
            [shevek.components.popup :refer [show-popup close-popup]]
            [shevek.reflow.core :refer [dispatch]]))

(defn- popup-content []
  [:div.ui.relaxed.middle.aligned.selection.list
   [:div.item {:on-click #(do (dispatch :viewer/raw-data-requested) (close-popup))}
    [:i.align.justify.icon]
    [:div.content (t :share/view-raw-data)]]
   [:div.item
    [:i.copy.icon]
    [:div.content (t :share/generate-url)]]])

(defn share-menu []
  [:a.icon.item {:on-click #(show-popup % popup-content {:position "bottom center"})}
   [:i.share.alternate.icon]])
