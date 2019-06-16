(ns shevek.components.virtualized
  (:require [reagent.core :as r]))

(defn- handle-scroll [first-window-row row-count event]
  (let [element (.-target event)
        scroll-height (.-scrollHeight element)
        scroll-top (.-scrollTop element)]
        ; client-height (.-clientHeight element)
    (reset! first-window-row (Math/floor (* (/ scroll-top scroll-height) row-count)))))

(defn- window-provider []
  (let [inner-window-start (r/atom 0)]
    (fn [{:keys [item-count item-height window-buffer]} render-fn]
      (let [on-scroll (partial handle-scroll inner-window-start item-count)
            content-height (* item-height item-count)
            window-size 10 ; TODO RV calculate on mount
            outer-window-start (max (- @inner-window-start window-buffer) 0)
            outer-window-end (min (+ @inner-window-start window-size window-buffer) item-count)
            window (range outer-window-start outer-window-end)
            spacer-height (* outer-window-start item-height)]
        [:div.scroll-area {:on-scroll on-scroll}
         [:div {:style {:height content-height}} ; Fake the full content so the scroll remaining is consistent
          [:div.spacer {:style {:height spacer-height}}]
          [render-fn window]]]))))

(defn virtual-table [{:keys [class header-count header-renderer row-count row-renderer row-height height window-buffer]
                      :or {header-count 1 window-buffer 10}}]
  [:div.virtual-table {:style {:height height}} ; TODO RV not sure why the height is needed as the parent div already has the height set
   (when header-renderer
     [:div
      [:table {:class class}
       [:thead
        (for [row-idx (range header-count)]
          ^{:key row-idx} [header-renderer {:row-idx row-idx}])]]])
   [window-provider
    {:item-count row-count :item-height row-height :window-buffer window-buffer}
    (fn [window]
      (println window)
      [:table {:class class}
       [:tbody
        (for [row-idx window]
          ^{:key row-idx} [row-renderer {:style {:height row-height} :row-idx row-idx}])]])]])
