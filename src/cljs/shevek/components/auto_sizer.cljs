(ns shevek.components.auto-sizer
  (:require [reagent.core :as r]))

(defn auto-sizer [render-fn]
  (let [dimensions (r/atom {:width 0 :height 0})
        on-resize (fn [event]
                    (let [parent (-> event .-target .-parentNode)]
                      (reset! dimensions {:width (.-offsetWidth parent)
                                          :height (.-offsetHeight parent)})))]
    (r/create-class
     {:display-name "auto-sizer"

      :component-did-mount
      (fn [this]
        (let [parent (-> this r/dom-node .-parentNode)]
          (js/addResizeListener parent on-resize)))

      :component-will-unmount
      (fn [this]
        (let [parent (-> this r/dom-node .-parentNode)]
          (js/removeResizeListener parent on-resize)))

      :reagent-render
      (fn [render-fn]
        (if (pos? (:width @dimensions))
          (render-fn @dimensions)
          [:div]))})))
