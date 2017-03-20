(ns pivot.cube-view.visualization
  (:require-macros [pivot.lib.reagent :refer [rfor]])
  (:require [reagent.core :as r]
            [clojure.string :as str]
            [pivot.i18n :refer [t]]
            [pivot.dw :as dw]
            [pivot.lib.react :refer [rmap]]
            [pivot.lib.collections :refer [detect]]
            [pivot.rpc :as rpc]
            [pivot.cube-view.shared :refer [panel-header cube-view format-measure format-dimension]]))

(defn- sort-results-according-to-selected-measures [result]
  (let [get-value-for-measure (fn [measure result]
                                (let [measure-value (last (detect #(= (:name measure) (name (first %))) result))]
                                  (format-measure (or measure-value 0) measure)))]
    (map #(assoc % :value (get-value-for-measure % result))
         (cube-view :measures))))

(defn- totals-visualization []
  (let [result (sort-results-according-to-selected-measures (first (cube-view :results :main)))]
    [:div.ui.statistics
     (for [{:keys [name title value]} result]
       ^{:key name}
       [:div.statistic
        [:div.label title]
        [:div.value value]])]))

(defn- calculate-rate [measure-value measure-name max-values]
  (let [rate (->> (max-values measure-name)
                  (/ measure-value)
                  (* 100))]
    (str rate "%")))

; FIXME solo anda para una dim x ahora
(defn- pivot-table-row [result split max-values]
  (let [dimension (first split)
        dimension-value (-> dimension :name keyword result)]
    [:tr
     [:td (format-dimension dimension-value dimension)]
     (rfor [measure (cube-view :measures)
            :let [measure-name (-> measure :name keyword)
                  measure-value (measure-name result)]]
       [:td.right.aligned
        [:div.bg {:style {:width (calculate-rate measure-value measure-name max-values)}}]
        (format-measure measure-value measure)])]))

(defn- calculate-max-values [measures results]
  (reduce (fn [max-values measure-name]
            (assoc max-values measure-name (apply max (map measure-name results))))
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
      (rmap (fn [result] [pivot-table-row result split max-values]) results)]]))

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
