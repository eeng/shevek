(ns shevek.components.virtualized
  (:require [reagent.core :as r]
            [shevek.components.auto-sizer :refer [auto-sizer]]))

(defn- calculate-inner-window-start [inner-window-start row-count event]
  (let [element (.-target event)
        scroll-height (.-scrollHeight element)
        scroll-top (.-scrollTop element)]
    (reset! inner-window-start (Math/floor (* (/ scroll-top scroll-height) row-count)))))

(defn- window-provider []
  (let [inner-window-start (r/atom 0)]
    (fn [{:keys [item-count item-height window-buffer window-height on-scroll]
          :or {on-scroll identity}}
         render-fn]
      [:div.scroll-area
       {:on-scroll #(do
                      (calculate-inner-window-start inner-window-start item-count %)
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

(defn- set-width [node width]
  (-> node js/$
      (.css "min-width" width)
      (.css "max-width" width)))

(defn- clear-column-widths
  "When the table updates with more header rows, the top ones could have the previous (no longer valid) widths"
  [vt]
  (-> vt (.find "thead tr:not(:last)") .children (.each #(set-width %2 ""))))

(defn- sync-column-widths
  "As two independent tables are used for headers and content, we need to programmatically fit the header column widths to the content."
  [vt]
  (let [first-content-row (-> vt (.find "tbody tr:first") .children .toArray)
        last-header-row (-> vt (.find "thead tr:last") .children .toArray)]
    (doseq [[content-cell header-cell] (map vector first-content-row last-header-row)
            :let [content-cell-width (-> content-cell js/$ .outerWidth)]
            :when content-cell-width]
      (set-width header-cell content-cell-width))))

(defn- sync-headers-and-content [vt-node]
  (when vt-node
    (let [vt (js/$ vt-node)]
      (clear-column-widths vt)
      (sync-column-widths vt))))

(defn- sync-headers-position
  [event]
  "As the user scroll horizontally, the headers' table needs to move as well to mantain the illusion that there is one table."
  (let [scroll-area (-> event .-target)]
    (.css (-> scroll-area js/$ (.closest ".virtual-table") (.find ".headers-table"))
          "margin-left" (-> scroll-area .-scrollLeft -))))

(defn headers [{:keys [header-count header-renderer row-height]}]
  (when header-renderer
    [:div.headers-area
     [:table.headers-table
      [:thead
       (for [row-idx (range header-count)]
         (header-renderer {:row-idx row-idx :style {:height row-height}}))]]]))

(defn windowed-content [{:keys [on-change]}]
  (r/create-class
   {:component-did-update on-change

    :reagent-render
    (fn [{:keys [row-renderer row-height window]}]
      [:table.content-table
       [:tbody
        (for [row-idx window]
          (row-renderer {:row-idx row-idx :style {:height row-height}}))]])}))

(defn virtual-table [{:keys [row-count row-renderer]}]
  {:pre [row-count row-renderer]}
  (let [vt (r/atom nil)]
    (fn [{:keys [class header-count header-renderer row-count row-renderer row-height window-buffer]
          :or {header-count 1 window-buffer 5}}]
      [auto-sizer
       (fn [{:keys [height]}]
         (let [window-height (- height (* header-count row-height))]
           [:div.virtual-table {:style {:height height}
                                :class class
                                :ref #(sync-headers-and-content (reset! vt %))}
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
                                  :on-change #(sync-headers-and-content @vt)}])]]))])))
