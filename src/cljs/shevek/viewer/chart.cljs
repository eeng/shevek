(ns shevek.viewer.chart
  (:require [cljsjs.chartjs]
            [reagent.core :as r]
            [shevek.viewer.shared :refer [format-dimension dimension-value format-measure]]
            [shevek.lib.collections :refer [index-of]]))

(def colors
  (cycle ["#42a5f5" "#ff7043" "#9ccc65" "#ffca28" "#8d6e63" "#5c6bc0" "#ef5350" "#66bb6a" "#ffee58"
          "#ec407a" "#ffa726" "#26a69a" "#ab47bc" "#26c6da" "#d4e157" "#7e57c2" "#78909c" "#81d4fa"]))

(defn- build-dataset-for-one-split [{:keys [title] :as measure} results viztype]
  (merge {:label title
          :data (map #(dimension-value measure %) results)}
         (case viztype
           :line-chart {:borderColor (first colors) :fill false}
           {:backgroundColor (take (count results) colors)})))

(defn- build-dataset-for-two-splits [{:keys [title] :as measure} results viztype split ds-idx]
  (merge {:label title
          :nestedLabels (map #(format-dimension (second split) %) results) ; Stored here for later use in tooltip-title
          :data (map #(dimension-value measure %) results)}
         (case viztype
           :line-chart {:borderColor (nth colors ds-idx) :fill false}
           {:backgroundColor (nth colors ds-idx)})))

(defn- build-datasets [measure split viztype results]
  (case (count split)
    1 [(build-dataset-for-one-split measure results viztype)]
    2 (let [transposed-subresults (apply map (fn [& args] args) (map :_results results))]
        (map-indexed #(build-dataset-for-two-splits measure %2 viztype split %1) transposed-subresults))))

(defn- build-chart-data [measure {:keys [results viztype] :as viewer}]
  (let [split (:split results)
        results (rest (:main results))] ; We don't need the totals row
    {:labels (map #(format-dimension (first split) %) results)
     :datasets (build-datasets measure split viztype results)}))

(def chart-types {:bar-chart "bar" :line-chart "line" :pie-chart "pie"})

; Necessary to make pie tooltips look like bar tooltips, as the default ones lack the dataset labels (our measures). Also it formats the measure values.
(defn- tooltip-label [{:keys [name] :as measure} tooltip-item data]
  (let [ds (get (.-datasets data) (.-datasetIndex tooltip-item))
        value (get (.-data ds) (.-index tooltip-item))]
    (str (.-label ds) ": " (format-measure measure {(keyword name) value}))))

(defn- tooltip-title [viztype tooltip-items data]
  (let [tooltip-item (first tooltip-items)
        ds (get (.-datasets data) (.-datasetIndex tooltip-item))
        labels (if (= viztype :pie-chart)
                 (.-labels data)
                 (or (.-nestedLabels ds) (.-labels data)))]
    (get labels (.-index tooltip-item))))

(defn- build-chart [canvas measure {:keys [viztype split] :as viewer}]
  (let [options (cond-> {:legend {:display false}
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

(defn chart-visualization [{:keys [measures results] :as viewer}]
  [:div
   (for [{:keys [name title] :as measure} measures]
     [:div.chart {:key name}
      [:div.title title ": " (format-measure measure (-> results :main first))]
      [chart measure viewer]])])
