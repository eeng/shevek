(ns shevek.components.virtualized
  (:require [reagent.core :as r]))

(defn- handle-scroll [first-window-row row-count event]
  (let [element (.-target event)
        scroll-height (.-scrollHeight element)
        scroll-top (.-scrollTop element)]
        ; client-height (.-clientHeight element)
    (reset! first-window-row (Math/floor (* (/ scroll-top scroll-height) row-count)))))

(defn virtual-table [_]
  (let [first-window-row (r/atom 0)]
    (fn [{:keys [class header-count header-renderer row-count row-renderer row-height height window-buffer]
          :or {header-count 1 window-buffer 10}}]
      (let [on-scroll (partial handle-scroll first-window-row row-count)
            full-height (* row-height row-count)
            window-size 10 ; TODO RV calculate on mount
            expanded-window-start (max (- @first-window-row window-buffer) 0)
            expanded-window-end (min (+ @first-window-row window-size window-buffer) row-count)
            window (range expanded-window-start expanded-window-end)
            spacer-height (* expanded-window-start row-height)]
        [:div.virtual-table {:style {:height height}} ; TODO RV not sure why the height is needed as the parent div already has the height set
         ; TODO RV los headers se estan rerenderizando en cada scroll event y no hace falta
         [:div
          [:table {:class class}
           (when header-renderer
             [:thead
              (for [row-idx (range header-count)]
                ^{:key row-idx} [header-renderer {:row-idx row-idx}])])]]
         [:div.scroll-area {:on-scroll on-scroll}
          [:div {:style {:height full-height}} ; Fake the full content so the scroll remaining is consistent
           [:div.spacer {:style {:height spacer-height}}]
           [:table {:class class}
            [:tbody
             (for [row-idx window]
               ^{:key row-idx} [row-renderer {:style {:height row-height} :row-idx row-idx}])]]]]]))))
