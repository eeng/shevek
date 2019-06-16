(ns shevek.components.virtualized
  (:require [reagent.core :as r]
            [shevek.components.auto-sizer :refer [auto-sizer]]))

(defn- handle-scroll [first-window-row row-count event]
  (let [element (.-target event)
        scroll-height (.-scrollHeight element)
        scroll-top (.-scrollTop element)]
    (reset! first-window-row (Math/floor (* (/ scroll-top scroll-height) row-count)))))

(defn- window-provider []
  (let [inner-window-start (r/atom 0)]
    (fn [{:keys [item-count item-height window-buffer window-height]} render-fn]
      (let [on-scroll (partial handle-scroll inner-window-start item-count)]
        [:div.scroll-area {:on-scroll on-scroll}
         (let [content-height (* item-height item-count)
               window-size (Math/ceil (* (/ window-height content-height) item-count))
               outer-window-start (max (- @inner-window-start window-buffer) 0)
               outer-window-end (min (+ @inner-window-start window-size window-buffer) item-count)
               window (range outer-window-start outer-window-end)
               spacer-height (* outer-window-start item-height)]
           [:div {:style {:height content-height}} ; Fake the full content so the scroll remaining is consistent
            [:div.spacer {:style {:height spacer-height}}]
            [render-fn window]])]))))

(defn virtual-table [{:keys [class header-count header-renderer row-count row-renderer row-height window-buffer]
                      :or {header-count 1 window-buffer 10}}]
  [auto-sizer
   (fn [{:keys [height]}]
     (let [row-props (fn [row-idx]
                       {:style {:height row-height} :row-idx row-idx})
           window-height (- height (* header-count row-height))]
       [:div.virtual-table {:style {:height height} :class class}
        (when header-renderer
          [:div
           [:table
            [:thead
             (for [row-idx (range header-count)]
               ^{:key row-idx} [header-renderer (row-props row-idx)])]]])
        [window-provider
         {:item-count row-count
          :item-height row-height
          :window-buffer window-buffer
          :window-height window-height}
         (fn [window]
           [:table
            [:tbody
             (for [row-idx window]
               ^{:key row-idx} [row-renderer (row-props row-idx)])]])]]))])
