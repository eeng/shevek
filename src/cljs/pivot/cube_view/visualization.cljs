(ns pivot.cube-view.visualization
  (:require-macros [pivot.lib.reagent :refer [rfor]])
  (:require [reagent.core :as r]
            [clojure.string :as str]
            [pivot.i18n :refer [t]]
            [pivot.dw :as dw]
            [pivot.lib.react :refer [rmap with-react-keys]]
            [pivot.lib.collections :refer [detect]]
            [pivot.rpc :as rpc]
            [pivot.cube-view.shared :refer [panel-header cube-view format-measure format-dimension totals-result?]]))

(defn- sort-results-according-to-selected-measures [result]
  (map #(assoc % :value (format-measure % result))
       (cube-view :measures)))

(defn- totals-visualization []
  (let [result (sort-results-according-to-selected-measures (first (cube-view :results :main)))]
    [:div.ui.statistics
     (for [{:keys [name title value]} result]
       ^{:key name}
       [:div.statistic
        [:div.label title]
        [:div.value value]])]))

(defn- calculate-rate [measure-value max-value]
  (let [rate (if (zero? max-value)
               0
               (/ measure-value max-value))]
    (str (* rate 100) "%")))

(defn- pivot-table-row [result dim depth measures max-values]
  [:tr
    [:td
     [:div {:class (str "depth-" depth)}
      (format-dimension dim result)]]
    (rfor [measure measures
           :let [measure-name (-> measure :name keyword)
                 measure-value (measure-name result)]]
      [:td.right.aligned
       [:div.bg (when-not (totals-result? result dim)
                  {:style {:width (calculate-rate measure-value (max-values measure-name))}})]
       (format-measure measure result)])])

(defn- pivot-table-rows [results [dim & dims] depth measures max-values]
  (when dim
    (mapcat #(into [(pivot-table-row % dim depth measures max-values)]
                   (pivot-table-rows (:_results %) dims (inc depth) measures max-values))
            results)))

(defn- calculate-max-values [measures results]
  (reduce (fn [max-values measure-name]
            (assoc max-values measure-name (->> results rest (map measure-name) (apply max))))
          {}
          (map (comp keyword :name) measures)))

(defn- pivot-table-visualization []
  (let [split (cube-view :split-arrived)
        measures (cube-view :measures)
        results (cube-view :results :main)
        max-values (calculate-max-values measures results)]
    [:table.ui.very.basic.compact.fixed.single.line.table
     [:thead
      [:tr
       [:th (->> split (map :title) (str/join ", "))]
       (rmap (fn [{:keys [title]}] [:th.right.aligned title]) measures)]]
     [:tbody
      (with-react-keys (pivot-table-rows results split 0 measures max-values))]]))

(defn visualization-panel []
  [:div.visualization.zone.panel.ui.basic.segment (rpc/loading-class [:results :main])
   (when (cube-view :results :main)
     (if (empty? (cube-view :measures))
       [:div.icon-hint
        [:i.warning.circle.icon]
        [:div.text (t :cubes/no-measures)]]
       (if (empty? (cube-view :split-arrived))
         [totals-visualization]
         [pivot-table-visualization])))])
