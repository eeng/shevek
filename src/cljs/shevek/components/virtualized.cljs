(ns shevek.components.virtualized
  (:require [reagent.core :as r]
            [shevek.components.auto-sizer :refer [auto-sizer]]))

(defn- calculate-first-window-row [first-window-row row-count event]
  (let [element (.-target event)
        scroll-height (.-scrollHeight element)
        scroll-top (.-scrollTop element)]
    (reset! first-window-row (Math/floor (* (/ scroll-top scroll-height) row-count)))))

(defn- window-provider []
  (let [inner-window-start (r/atom 0)]
    (fn [{:keys [item-count item-height window-buffer window-height on-scroll]} render-fn]
      [:div.scroll-area
       {:on-scroll #(do
                      (calculate-first-window-row inner-window-start item-count %)
                      (on-scroll %))}
       (let [content-height (* item-height item-count)
             window-size (Math/ceil (* (/ window-height content-height) item-count))
             outer-window-start (max (- @inner-window-start window-buffer) 0)
             outer-window-end (min (+ @inner-window-start window-size window-buffer) item-count)
             window (range outer-window-start outer-window-end)
             spacer-height (* outer-window-start item-height)]
         [:div {:style {:height content-height}} ; Fake the full content so the scroll remaining is consistent
          [:div.spacer {:style {:height spacer-height}}]
          [render-fn window]])])))

(defn- sync-column-widths
  [vt-node]
  "As two independent tables are used for headers and content, we need to programmatically fit the header column widths to the content."
  (when vt-node
    (let [element (js/$ vt-node)
          first-content-row (-> element (.find "tbody tr:first") .children .toArray)
          last-header-row (-> element (.find "thead tr:last") .children .toArray)]
      (doseq [[content-cell header-cell] (map vector first-content-row last-header-row)
              :let [content-width (-> content-cell js/$ .width)]
              :when content-width]
        (-> header-cell js/$ (.width content-width))))))

(defn- copy-content-width
  [node]
  "Sets the headers table width to the same content table width. This is needed so the individual cell's widths sets by the sync-column-widths are respected when content overflows horizontally."
  (when node
    (let [vt (-> node js/$ (.closest ".virtual-table"))
          content-width (-> vt (.find ".content-table") .width)]
      (-> vt (.find ".headers-table") (.width content-width)))))

(defn- sync-headers-position
  [event]
  "As the user scroll horizontally, the headers' table needs to move as well to mantain the illusion that there is one table."
  (let [scroll-area (-> event .-target)]
    (.css (-> scroll-area js/$ (.closest ".virtual-table") (.find ".headers-table"))
          "margin-left" (-> scroll-area .-scrollLeft -))))

(defn headers [{:keys [header-count header-renderer row-height]}]
  (when header-renderer
    [:div.headers-area
     [:table.headers-table {:ref copy-content-width}
      [:thead
       (for [row-idx (range header-count)]
         ^{:key row-idx} [header-renderer {:row-idx row-idx :style {:height row-height}}])]]]))

(defn windowed-content [{:keys [on-change]}]
  (r/create-class
   {:component-did-update on-change

    :reagent-render
    (fn [{:keys [row-renderer row-height window]}]
      [:table.content-table
       [:tbody
        (for [row-idx window]
          ^{:key row-idx} [row-renderer {:row-idx row-idx :style {:height row-height}}])]])}))

(defn virtual-table []
  (let [vt (r/atom nil)]
    (fn [{:keys [class header-count header-renderer row-count row-renderer row-height window-buffer]
          :or {header-count 1 window-buffer 10}}]
      [auto-sizer
       (fn [{:keys [height]}]
         (let [window-height (- height (* header-count row-height))]
           [:div.virtual-table {:style {:height height}
                                :class class
                                :ref #(sync-column-widths (reset! vt %))}
            [headers {:header-count header-count
                      :header-renderer header-renderer
                      :row-height row-height}]
            [window-provider
             {:item-count row-count
              :item-height row-height
              :window-buffer window-buffer
              :window-height window-height
              :on-scroll sync-headers-position}
             (fn [window]
               [windowed-content {:window window
                                  :row-renderer row-renderer
                                  :row-height row-height
                                  :on-change #(sync-column-widths @vt)}])]]))])))
