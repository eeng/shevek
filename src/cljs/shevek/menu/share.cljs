(ns shevek.menu.share
  (:require [shevek.i18n :refer [t]]
            [shevek.components.popup :refer [show-popup close-popup]]
            [shevek.reflow.core :refer [dispatch]]))

(defn- popup-content []
  [:div.ui.relaxed.middle.aligned.selection.list
   [:a.item {:on-click #(do (dispatch :viewer/raw-data-requested) (close-popup))}
    [:i.align.justify.icon]
    [:div.content (t :raw-data/menu)]]])

(defn share-menu []
  [:a.icon.item {:on-click #(show-popup % popup-content {:position "bottom center"})
                 :title (t :share/title)}
   [:i.share.alternate.icon]])
