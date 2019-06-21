(ns shevek.components.virtualized
  (:require [reagent.core :as r]
            [shevek.components.auto-sizer :refer [auto-sizer]]))

(defn- set-width [node width]
  (-> node js/$
      (.css "min-width" width)
      (.css "max-width" width)))

(defn- clear-previous-column-widths [vt-node]
  (-> vt-node (.find "thead tr") .children (.each #(set-width %2 ""))))

(defn- read-column-widths [vt-node]
  (let [first-content-row (-> vt-node (.find "tbody tr:first") .children .toArray)
        last-header-row (-> vt-node (.find "thead tr:last") .children .toArray)]
    (vec
     (for [[content-cell header-cell] (map vector first-content-row last-header-row)
           :let [content-cell-width (-> content-cell js/$ .outerWidth)
                 header-cell-width (-> header-cell js/$ .outerWidth)]]
       (max header-cell-width content-cell-width)))))

(defn- set-column-widths
  [{:keys [vt-node column-widths]}]
  "Every time the window change we need to resize the columns to the initial widths to prevent the browser's automatic resizing which would produce a bad user experience."
  (let [first-content-row (-> vt-node (.find "tbody tr:first") .children .toArray)
        last-header-row (-> vt-node (.find "thead tr:last") .children .toArray)]
    (doseq [[header-cell content-cell width] (map vector last-header-row first-content-row column-widths)]
      (set-width header-cell width)
      (set-width content-cell width))))

(defn- sync-headers-position
  [event]
  "As the user scroll horizontally, the headers' table needs to move as well to mantain the illusion that there is one table."
  (let [scroll-area (-> event .-target)]
    (.css (-> scroll-area js/$ (.closest ".virtual-table") (.find ".headers-table"))
          "margin-left" (-> scroll-area .-scrollLeft -))))

(defn- calculate-inner-window-start [event {:keys [item-count]}]
  (let [element (.-target event)
        scroll-height (.-scrollHeight element)
        scroll-top (.-scrollTop element)]
    (Math/floor (* (/ scroll-top scroll-height) item-count))))

(defn- calculate-window-size [{:keys [item-count item-height window-height]}]
  (let [content-height (* item-height item-count)]
    (Math/ceil (* (/ window-height content-height) item-count))))

(defn- calculate-inner-window [inner-window-start props]
  (let [window-size (calculate-window-size props)
        inner-window-end (+ inner-window-start window-size)]
    {:start inner-window-start :end inner-window-end}))

(defn- calculate-outer-window [inner-window {:keys [window-buffer]}]
  {:start (- (inner-window :start) window-buffer)
   :end (+ (inner-window :end) window-buffer)})

(defn- update-outer-window [outer-window inner-window {:keys [window-buffer slide-window-at] :as props}]
  (let [buffer-remining (- window-buffer slide-window-at)]
    (if (or (nil? outer-window)
            (>= (inner-window :end) (- (outer-window :end) buffer-remining))
            (<= (inner-window :start) (+ (outer-window :start) buffer-remining)))
      (calculate-outer-window inner-window props)
      outer-window)))

(defn- window-provider []
  (let [outer-window (r/atom nil)]
    (fn [{:keys [item-count item-height on-scroll] :or {on-scroll identity} :as props} render-fn]
      [:div.scroll-area
       {:on-scroll #(let [inner-window-start (calculate-inner-window-start % props)
                          inner-window (calculate-inner-window inner-window-start props)]
                      (swap! outer-window update-outer-window inner-window props)
                      (on-scroll %))}
       (let [{:keys [start end]} (or @outer-window
                                     (-> (calculate-inner-window 0 props) (calculate-outer-window props)))
             start (max start 0)
             end (min end item-count)
             window (range start end)
             content-height (* item-height item-count)
             spacer-height (* start item-height)]
         [:div {:style {:height content-height ; Fake the full content so the scroll remaining is consistent
                        :padding-top spacer-height}}
          [render-fn window]])])))

(defn windowed-content [{:keys [on-change] :or {on-change identity}}]
  (r/create-class
   {:component-did-update on-change

    :reagent-render
    (fn [{:keys [row-renderer row-height window]}]
      [:table.content-table
       [:tbody
        (for [row-idx window]
          (row-renderer {:row-idx row-idx :style {:height row-height}}))]])}))

(defn headers [{:keys [header-count header-renderer row-height]}]
  (when header-renderer
    [:div.headers-area
     [:table.headers-table
      [:thead
       (for [row-idx (range header-count)]
         (header-renderer {:row-idx row-idx :style {:height row-height}}))]]]))

(defn auto-sized-virtual-table
  "When the virtual table mounts and updates (but not when scrolling) we store the column automatically calculated by the browser so afterwards when the window changes, we can used those instead of the newly calculated by the browser. Otherwise the columns would move around while scrolling. This works because the pivot table contains as a first row the biggest values, which should prevent most text overflows."
  []
  (let [state (r/atom {:vt-node nil :column-widths []})]
    (r/create-class
     {:component-did-mount
      (fn [this]
        (let [vt (-> this r/dom-node js/$)]
          (reset! state {:vt-node vt :column-widths (read-column-widths vt)})
          (set-column-widths @state)))

      :component-did-update
      (fn [this]
        (clear-previous-column-widths (:vt-node @state))
        (swap! state assoc :column-widths (read-column-widths (:vt-node @state)))
        (set-column-widths @state))

      :reagent-render
      (fn [{:keys [height class header-count header-renderer row-count row-renderer row-height window-buffer slide-window-at]
            :or {header-count 1 window-buffer 10 slide-window-at (Math/round (* window-buffer 0.8))}}]
        {:pre [row-count row-renderer (<= slide-window-at window-buffer)]}
        (let [window-height (- height (* header-count row-height))]
          [:div.virtual-table {:style {:height height}
                               :class class}
           [headers {:header-count header-count
                     :header-renderer header-renderer
                     :row-height row-height}]
           [window-provider
            {:item-count row-count
             :item-height row-height
             :window-buffer window-buffer
             :slide-window-at slide-window-at
             :window-height window-height
             :on-scroll sync-headers-position}
            (fn [window]
              [windowed-content {:window window
                                 :row-renderer row-renderer
                                 :row-height row-height
                                 :on-change #(set-column-widths @state)}])]]))})))

(defn virtual-table [props]
  [auto-sizer
   (fn [dimensions]
     [auto-sized-virtual-table (merge props dimensions)])])
