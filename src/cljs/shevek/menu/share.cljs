(ns shevek.menu.share
  (:require [reagent.core :as r]
            [shevek.i18n :refer [t]]
            [shevek.components.popup :refer [show-popup close-popup]]
            [shevek.reflow.core :refer [dispatch]]
            [shevek.notification :refer [notify]]
            [cljsjs.clipboard]))

(defn clipboard-button [button]
  (let [clipboard-atom (atom nil)
        get-url #(.-href js/location)]
    (r/create-class
     {:component-did-mount #(let [clipboard (new js/Clipboard (r/dom-node %) #js {:text get-url})]
                              (reset! clipboard-atom clipboard))
      :component-will-unmount #(when @clipboard-atom
                                 (.destroy @clipboard-atom)
                                 (reset! clipboard-atom nil))
      :reagent-render (fn [] button)})))

(defn- popup-content []
  [:div.ui.relaxed.middle.aligned.selection.list
   [clipboard-button
    [:a.item {:on-click #(do (notify (t :share/copied)) (close-popup))}
     [:i.copy.icon]
     [:div.content (t :share/copy-url)]]]
   [:a.item {:on-click #(do (dispatch :viewer/raw-data-requested) (close-popup))}
    [:i.align.justify.icon]
    [:div.content (t :raw-data/menu)]]])

(defn share-menu []
  [:a.icon.item {:on-click #(show-popup % popup-content {:position "bottom center"})
                 :title (t :share/title)}
   [:i.share.alternate.icon]])
