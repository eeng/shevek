(ns shevek.components.auto-sizer
  (:require [reagent.core :as r]
            [add-resize-listener]))

(defn auto-sizer [render-fn]
  (let [dimensions (r/atom {:width 0 :height 0})
        on-resize (fn [event]
                    (let [parent (-> event .-target .-parentNode)]
                      (reset! dimensions {:width (.-offsetWidth parent)
                                          :height (.-offsetHeight parent)})))
        unsubscribe-listener (r/atom nil)]
    (r/create-class
     {:display-name "auto-sizer"

      :component-did-mount
      (fn [this]
        (let [parent (-> this r/dom-node .-parentNode)]
          (reset! unsubscribe-listener (add-resize-listener parent on-resize))))

      :component-will-unmount
      (fn [this]
        (@unsubscribe-listener))

      :reagent-render
      (fn [render-fn]
        (if (pos? (:width @dimensions))
          (render-fn @dimensions)
          [:div]))})))
