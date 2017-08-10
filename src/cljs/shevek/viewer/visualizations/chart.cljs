(ns shevek.viewer.visualizations.chart
  (:require [cljsjs.chartjs]
            [reagent.core :as r]
            [shevek.viewer.shared :refer [format-dimension dimension-value format-measure]]
            [shevek.i18n :refer [t]]))

(def colors
  (cycle ["#42a5f5" "#ff7043" "#9ccc65" "#ffca28" "#8d6e63" "#5c6bc0" "#ef5350" "#66bb6a" "#ffee58"
          "#ec407a" "#ffa726" "#26a69a" "#ab47bc" "#26c6da" "#d4e157" "#7e57c2" "#78909c" "#81d4fa"]))

(def chart-types {:bar-chart "bar" :line-chart "line" :pie-chart "pie"})

(defn- build-dataset-for-one-split [{:keys [title] :as measure} results viztype]
  (merge {:label title
          :data (map #(dimension-value measure %) results)}
         (case viztype
           :line-chart {:borderColor (first colors) :backgroundColor "rgba(66, 165, 245, 0.3)"}
           {:backgroundColor (take (count results) colors)})))

(defn- build-dataset-for-two-splits [{:keys [title] :as measure} results viztype split ds-idx]
  (let [labels (map #(format-dimension (second split) %) results)]
    (merge {:label (first labels)
            :nestedLabels labels ; Stored here for later use in tooltip-title
            :data (map #(dimension-value measure %) results)}
           (case viztype
             :line-chart {:borderColor (nth colors ds-idx) :fill false}
             {:backgroundColor (nth colors ds-idx)}))))

(defn- fill-vector [size coll]
  (for [i (range size)]
    (get (vec coll) i)))

(defn- build-datasets [measure split viztype results]
  (case (count split)
    1 [(build-dataset-for-one-split measure results viztype)]
    2 (let [subresults (map :_results results)
            biggest-size (->> subresults (map count) (apply max))
            filled-subresults (map #(fill-vector biggest-size %) subresults)
            transposed-subresults (apply map (fn [& args] args) filled-subresults)]
        (map-indexed #(build-dataset-for-two-splits measure %2 viztype split %1) transposed-subresults))))

(defn build-chart-data [measure {:keys [results] :as viewer}]
  (let [{:keys [viztype split main]} results
        results (rest main)] ; We don't need the totals row
    {:labels (map #(format-dimension (first split) %) results)
     :datasets (build-datasets measure split viztype results)}))

; Necessary to make pie tooltips look like bar tooltips, as the default ones lack the dataset labels (our measures). Also it formats the measure values.
(defn- tooltip-label [{:keys [name] :as measure} tooltip-item data]
  (let [ds (get (.-datasets data) (.-datasetIndex tooltip-item))
        value (get (.-data ds) (.-index tooltip-item))]
    (str " " (.-label ds) ": " (format-measure measure {(keyword name) value}))))

(defn- tooltip-title [viztype tooltip-items data]
  (let [tooltip-item (first tooltip-items)
        ds (get (.-datasets data) (.-datasetIndex tooltip-item))
        idx (.-index tooltip-item)]
    (if (.-nestedLabels ds)
      (str (get (.-nestedLabels ds) idx) " ‧ " (get (.-labels data) idx))
      (get (.-labels data) idx))))

(defn- build-chart [canvas {:keys [title] :as measure} {:keys [viztype results] :as viewer}]
  (let [chart-title (str title ": " (format-measure measure (-> results :main first)))
        show-legend? (or (> (-> results :split count) 1)
                         (= viztype :pie-chart))
        options (cond-> {:title {:display true :text chart-title}
                         :legend {:display show-legend? :position "bottom"}
                         :maintainAspectRatio false
                         :tooltips {:callbacks {:label (partial tooltip-label measure) :title (partial tooltip-title viztype)}}}
                        (not= viztype :pie-chart) (assoc :scales {:yAxes [{:ticks {:beginAtZero true} :position "right"}]}))]
    (js/Chart. canvas
               (clj->js {:type (chart-types viztype)
                         :data (build-chart-data measure viewer)
                         :options options}))))

(defn- update-chart [chart measure viewer]
  (aset chart "data" (clj->js (build-chart-data measure viewer)))
  (.update chart 0))

(defn- chart [measure viewer]
  (let [chart (atom nil)]
    (r/create-class {:reagent-render (fn [_] [:canvas])
                     :component-did-mount #(reset! chart (build-chart (r/dom-node %) measure viewer))
                     :component-did-update #(apply update-chart @chart (r/props %) (r/children %))
                     :component-will-unmount #(do (.destroy @chart) (reset! chart nil))})))

(defn- set-chart-height [component measures-count]
  (let [chart (-> component r/dom-node js/$)
        viz-height (-> chart (.closest ".visualization-container") .height)
        margin-bottom 10]
    (.css chart "height" (str "calc(" (/ 100 measures-count) "% - " margin-bottom "px)"))))

; Chart.js doesn't allow to update the type so we need to remount on viztype change, hence that :key.
; Also when split count change because the tooltips title callbacks are installed only on mount
(defn chart-visualization [{:keys [measures results] :as viewer}]
  (let [{:keys [viztype split]} results]
    (if (> (count split) 2)
      [:div.icon-hint
       [:i.warning.circle.icon]
       [:div.text (t :viewer/too-many-splits-for-chart)]]
      [:div.charts
       (for [{:keys [name title] :as measure} measures]
         [:div.chart-container {:key name :ref #(when % (set-chart-height % (count measures)))}
          ^{:key (str viztype (count split))} [chart measure viewer]])])))
