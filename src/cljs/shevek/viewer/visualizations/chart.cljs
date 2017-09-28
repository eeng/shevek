(ns shevek.viewer.visualizations.chart
  (:require [cljsjs.chartjs]
            [reagent.core :as r]
            [shevek.viewer.shared :refer [format-dimension dimension-value format-measure]]
            [shevek.i18n :refer [t]]))

(def colors
  (cycle ["#42a5f5" "#ff7043" "#9ccc65" "#ffca28" "#8d6e63" "#5c6bc0" "#ef5350" "#66bb6a" "#ffee58"
          "#ec407a" "#ffa726" "#26a69a" "#ab47bc" "#26c6da" "#d4e157" "#7e57c2" "#78909c" "#81d4fa"]))

(def chart-types {:bar-chart "bar" :line-chart "line" :pie-chart "pie"})

(defn- build-data [measure results viztype]
  (for [result results :let [value (dimension-value measure result)]]
    (if (and (= :pie-chart viztype) (neg? value))
      0
      value)))

(defn- build-dataset-for-one-split [{:keys [title] :as measure} results viztype]
  (merge {:label title
          :data (build-data measure results viztype)}
         (case viztype
           :line-chart {:borderColor (first colors) :backgroundColor "rgba(66, 165, 245, 0.3)"}
           {:backgroundColor (take (count results) colors)})))

(defn- build-dataset-for-two-splits [{:keys [title] :as measure} results viztype splits ds-idx]
  (let [labels (map #(format-dimension (second splits) %) results)]
    (merge {:label (first labels)
            :nestedLabels labels ; Stored here for later use in tooltip-title
            :data (build-data measure results viztype)}
           (case viztype
             :line-chart {:borderColor (nth colors ds-idx) :fill false}
             {:backgroundColor (nth colors ds-idx)}))))

(defn- fill-vector [size coll]
  (for [i (range size)]
    (get (vec coll) i)))

(defn- build-datasets [measure {:keys [splits viztype results]}]
  (case (count splits)
    1 [(build-dataset-for-one-split measure results viztype)]
    2 (let [subresults (map #(or (:child-rows %) (:child-cols %)) results)
            biggest-size (->> subresults (map count) (apply max))
            filled-subresults (map #(fill-vector biggest-size %) subresults)
            transposed-subresults (apply map (fn [& args] args) filled-subresults)]
        (map-indexed #(build-dataset-for-two-splits measure %2 viztype splits %1) transposed-subresults))))

(defn build-chart-data [measure {:keys [splits] :as viz}]
  (let [viz (update viz :results rest)] ; We don't need the totals row
    {:labels (map #(format-dimension (first splits) %) (:results viz))
     :datasets (build-datasets measure viz)}))

; Necessary to make pie tooltips look like bar tooltips, as the default ones lack the dataset labels (our measures). Also it formats the measure values.
(defn- tooltip-label [{:keys [name title] :as measure} tooltip-item data]
  (let [ds (get (aget data "datasets") (aget tooltip-item "datasetIndex"))
        value (get (aget ds "data") (aget tooltip-item "index"))]
    (str " " title ": " (format-measure measure {(keyword name) value}))))

(defn- tooltip-title [viztype tooltip-items data]
  (let [tooltip-item (first tooltip-items)
        ds (get (aget data "datasets") (aget tooltip-item "datasetIndex"))
        idx (aget tooltip-item "index")
        labels (aget data "labels")
        nested-labels (aget ds "nestedLabels")]
    (if nested-labels
      (str (get nested-labels idx) " ‧ " (get labels idx))
      (get labels idx))))

(defn- build-chart-opts [{:keys [title] :as measure} {:keys [viztype splits results] :as viz}]
  (let [chart-title (str title ": " (format-measure measure (first results)))
        show-legend? (or (> (count splits) 1)
                         (= viztype :pie-chart))]
    (cond-> {:title {:display true :text chart-title}
             :legend {:display show-legend? :position "bottom"}
             :maintainAspectRatio false
             :tooltips {:callbacks {:label (partial tooltip-label measure) :title (partial tooltip-title viztype)}}}
            (not= viztype :pie-chart) (assoc :scales {:yAxes [{:ticks {:beginAtZero true} :position "right"}]}))))

(defn- build-chart [canvas measure {:keys [viztype] :as viz}]
  (js/Chart. canvas (clj->js {:type (chart-types viztype)
                              :data (build-chart-data measure viz)
                              :options (build-chart-opts measure viz)})))

(defn- update-chart [chart measure viz]
  (aset chart "data" (clj->js (build-chart-data measure viz)))
  (aset chart "options" "title" (clj->js (:title (build-chart-opts measure viz))))
  (.update chart 0))

(defn- chart [measure viz]
  (let [chart (atom nil)]
    (r/create-class {:reagent-render (fn [_] [:canvas])
                     :component-did-mount #(reset! chart (build-chart (r/dom-node %) measure viz))
                     :component-did-update #(apply update-chart @chart (r/props %) (r/children %))
                     :component-will-unmount #(do (.destroy @chart) (reset! chart nil))})))

(defn- set-chart-height [component measures-count]
  (let [chart (-> component r/dom-node js/$)
        viz-height (-> chart (.closest ".visualization-container") .height)
        margin-bottom 10]
    (.css chart "height" (str "calc(" (/ 100 measures-count) "% - " margin-bottom "px)"))))

; Chart.js doesn't allow to update the type so we need to remount on viztype change, hence that :key.
; Also when split count change because the tooltips title callbacks are installed only on mount
(defn chart-visualization [{:keys [measures viztype splits] :as viz}]
  (if (> (count splits) 2)
    [:div.icon-hint
     [:i.warning.circle.icon]
     [:div.text (t :viewer/too-many-splits-for-chart)]]
    [:div.charts
     (for [{:keys [name] :as measure} measures]
       [:div.chart-container {:key name :ref #(when % (set-chart-height % (count measures)))}
        ^{:key (str viztype (count splits))} [chart measure viz]])]))
