(ns shevek.components.auto-sizer
  (:require [reagent.core :as r]
            [add-resize-listener]))

(defn auto-sizer [render-fn]
  (let [dimensions (r/atom {:width 0 :height 0})
        on-resize (fn [event]
                    (let [target (-> event .-target)]
                      (reset! dimensions {:width (.-offsetWidth target)
                                          :height (.-offsetHeight target)})))
        unsubscribe-listener (r/atom nil)]
    (r/create-class
     {:display-name "auto-sizer"

      :component-did-mount
      (fn [this]
        (let [resizing-container (-> this r/dom-node)]
          (reset! unsubscribe-listener (add-resize-listener resizing-container on-resize))))

      :component-will-unmount
      (fn [this]
        (@unsubscribe-listener))

      :reagent-render
      (fn [render-fn]
        [:div.auto-sizer {:style {:height "100%"}} ; To attach (on mount) and detach (on unmount) the resize-listener. Previously we use the parent node so this wasn't needed but if the parent changed (do to changing the viztype for example) the unsubscribe couldn't find it anymore.
         (when (pos? (:width @dimensions))
           (render-fn @dimensions))])})))
