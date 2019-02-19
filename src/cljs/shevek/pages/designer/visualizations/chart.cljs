(ns shevek.pages.designer.visualizations.chart
  (:require [cljsjs.chartjs]
            [reagent.core :as r]
            [shevek.domain.dw :refer [format-dimension measure-value format-measure]]
            [shevek.i18n :refer [t]]
            [shevek.lib.collections :refer [detect]]))

(defn- transpose-matrix [matrix]
  (if (seq matrix)
    (apply map (fn [& args] args) matrix)
    []))

(def colors
  (cycle ["#42a5f5" "#ff7043" "#9ccc65" "#ffca28" "#8d6e63" "#5c6bc0" "#ef5350" "#66bb6a" "#ffee58"
          "#ec407a" "#ffa726" "#26a69a" "#ab47bc" "#26c6da" "#d4e157" "#7e57c2" "#78909c" "#81d4fa"]))

(def chart-types {:bar-chart {:js-type "bar"
                              :background-alpha "AA"
                              :hover-alpha "DD"
                              :js-opts {:borderWidth 2}}
                  :line-chart {:js-type "line"
                               :background-alpha "22"
                               :legend-position "bottom"
                               :js-opts {:borderWidth 2 :pointRadius 1}}
                  :pie-chart {:js-type "pie"
                              :background-alpha "DD"
                              :hover-alpha "FF"
                              :legend-position "right"
                              :js-opts {:borderWidth 1}}})

(defn- build-data [measure results viztype]
  (for [result results :let [value (measure-value measure result)]]
    (if (or (nil? value)
            (and (= :pie-chart viztype) (neg? value)))
      0
      value)))

(defn- transparent [color alpha]
  (if (coll? color)
    (map #(transparent % alpha) color)
    (str color alpha)))

(defn- reorganize-results [{:keys [splits results]}]
  (case (count splits)
    1 [nil [(rest results)]]
    2 (let [subresults-matrix (map :child-cols (rest results))
            second-split (second splits)
            second-split-values (->> (first results) :child-cols (map #(format-dimension second-split %)))
            filled-subresults (for [results subresults-matrix]
                                (for [ssv second-split-values]
                                  (detect #(= (format-dimension second-split %) ssv) results)))]
        [second-split-values (transpose-matrix filled-subresults)])))

(defn- build-dataset [measure results {:keys [viztype]} ds-idx ds-labels]
  (let [data (build-data measure results viztype)
        color (if (or (= viztype :line-chart) ds-labels)
                (nth colors ds-idx)
                (take (count data) colors))
        {:keys [background-alpha hover-alpha js-opts]} (chart-types viztype)]
    (merge
      {:data data
       :label (nth ds-labels ds-idx)
       :borderColor color
       :backgroundColor (transparent color background-alpha)
       :hoverBackgroundColor (transparent color hover-alpha)}
      js-opts)))

(defn- build-datasets [measure viz]
  (let [[ds-labels results-matrix] (reorganize-results viz)]
    (for [[ds-idx results] (map-indexed vector results-matrix)]
      (build-dataset measure results viz ds-idx ds-labels))))

(defn build-chart-data [measure {:keys [splits results] :as viz}]
  (let [labels (map #(format-dimension (first splits) %) (rest results))
        datasets (build-datasets measure viz)]
    {:labels labels :datasets datasets}))

; Necessary to make pie tooltips look like bar tooltips, as the default ones lack the dataset labels (our measures). Also it formats the measure values.
(defn- tooltip-label [{:keys [name title] :as measure} tooltip-item data]
  (let [ds (get (aget data "datasets") (aget tooltip-item "datasetIndex"))
        value (get (aget ds "data") (aget tooltip-item "index"))]
    (str " " title ": " (format-measure measure {(keyword name) value}))))

; The default tooltip doesn't add the dataset label
(defn- tooltip-title [viztype tooltip-items data]
  (let [tooltip-item (first tooltip-items)
        ds (get (aget data "datasets") (aget tooltip-item "datasetIndex"))
        idx (aget tooltip-item "index")
        labels (aget data "labels")
        ds-label (aget ds "label")]
    (if ds-label
      (str (get labels idx) " ‧ " ds-label)
      (get labels idx))))

(defn- build-chart-opts [{:keys [title] :as measure} {:keys [viztype splits results]}]
  (let [chart-title (str title ": " (format-measure measure (first results)))
        show-legend? (or (> (count splits) 1)
                         (= viztype :pie-chart))]
    (cond-> {:title {:display true :text chart-title}
             :legend {:display show-legend? :position (:legend-position (chart-types viztype))}
             :maintainAspectRatio false
             :tooltips {:callbacks {:label (partial tooltip-label measure) :title (partial tooltip-title viztype)}}}
            (not= viztype :pie-chart) (assoc :scales {:yAxes [{:ticks {:beginAtZero true} :position "right"}]}))))

(defn- build-chart [canvas measure {:keys [viztype] :as viz}]
  (js/setTimeout #(-> canvas js/$ (.removeClass "hidden")) 50) ; Give some time for the resizing (set-chart-height) to finish and then do a smooth transition
  (js/Chart. canvas (clj->js {:type ((chart-types viztype) :js-type)
                              :data (build-chart-data measure viz)
                              :options (build-chart-opts measure viz)})))

(defn- update-chart [chart measure viz]
  (aset chart "data" (clj->js (build-chart-data measure viz)))
  (aset chart "options" "title" (clj->js (:title (build-chart-opts measure viz))))
  (.update chart 0))

(defn- chart [measure viz]
  (let [chart (atom nil)]
    (r/create-class {:reagent-render (fn [_] [:canvas.hidden])
                     :component-did-mount #(reset! chart (build-chart (r/dom-node %) measure viz))
                     :component-did-update #(apply update-chart @chart (r/props %) (r/children %))
                     :component-will-unmount #(do (.destroy @chart) (reset! chart nil))})))

(defn- set-chart-height [component measures-count]
  (let [chart (-> component r/dom-node js/$)
        margin-bottom 10]
    (.css chart "height" (str "calc(" (/ 100 measures-count) "% - " margin-bottom "px)"))))

; Chart.js doesn't allow to update the type so we need to remount on viztype change, hence that :key.
; Also when split count change because the tooltips title callbacks are installed only on mount
(defn chart-visualization [{:keys [measures viztype splits] :as viz}]
  (cond
    (> (count splits) 2)
    [:div.icon-hint
     [:i.warning.circle.icon]
     [:div.text (t :designer/too-many-splits-for-chart)]]

    (and (= (count splits) 2) (= (:on (second splits)) "rows")) ; We need the child-cols of the grand-total result to get a list of all second split values, as one dataset is generated for each one
    [:div.icon-hint
     [:i.warning.circle.icon]
     [:div.text (t :designer/chart-with-second-split-on-rows)]]

    :else
    [:div.charts
     (for [{:keys [name] :as measure} measures]
       [:div.chart-container {:key name :ref #(when % (set-chart-height % (count measures)))}
        ^{:key (str viztype (count splits))} [chart measure viz]])]))
