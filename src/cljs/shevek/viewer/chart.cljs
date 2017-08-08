(ns shevek.viewer.chart
  (:require [cljsjs.chartjs]
            [reagent.core :as r]
            [shevek.viewer.shared :refer [format-dimension dimension-value format-measure]]
            [shevek.lib.collections :refer [index-of]]))

(def colors
  (cycle ["#ef5350" "#ec407a" "#ab47bc" "#7e57c2" "#5c6bc0" "#42a5f5" "#81d4fa" "#26c6da" "#26a69a"
          "#66bb6a" "#9ccc65" "#d4e157" "#ffee58" "#ffca28" "#ffa726" "#ff7043" "#8d6e63" "#78909c"]))

(defn- viztype-dependant-dataset-opts [viztype results measure measures]
  (case viztype
    :line-chart {:fill false
                 :borderColor (nth colors (index-of measures measure))}
    {:backgroundColor (take (count results) colors)}))

(defn- build-dataset [{:keys [title] :as measure} results measures viztype]
  (merge {:label title
          :data (map #(dimension-value measure %) results)}
         (viztype-dependant-dataset-opts viztype results measure measures)))

(defn- build-chart-data [measure {:keys [measures results viztype] :as viewer}]
  (let [split (:split results)
        results (rest (:main results)) ; We don't need the totals row
        labels (map #(format-dimension (first split) %) results) ; TODO funca para un solo split x ahora
        dataset (build-dataset measure results measures viztype)]
    {:labels labels :datasets [dataset]}))

(def chart-types {:bar-chart "bar"
                  :line-chart "line"
                  :pie-chart "pie"})

; This two guys will make pie tooltips look like bar tooltips, as the default ones lack the dataset labels (our measures)
(defn- pie-tooltip-title [tooltip-items data]
  (get (.-labels data) (.-index (first tooltip-items))))

(defn- pie-tooltip-label [tooltip-item data]
  (let [ds (get (.-datasets data) (.-datasetIndex tooltip-item))
        value (get (.-data ds) (.-index tooltip-item))]
    (str (.-label ds) ": " value)))

(defn- build-chart [canvas measure {:keys [viztype] :as viewer}]
  (let [options (case viztype
                  :pie-chart {:tooltips {:callbacks {:title pie-tooltip-title :label pie-tooltip-label}}}
                  {:scales {:yAxes [{:ticks {:beginAtZero true} :position "right"}]}})]
    (js/Chart. canvas
               (clj->js {:type (chart-types viztype)
                         :data (build-chart-data measure viewer)
                         :options (assoc options :legend {:display false})}))))

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
