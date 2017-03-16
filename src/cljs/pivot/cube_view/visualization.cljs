(ns pivot.cube-view.visualization
  (:require [reagent.core :as r]
            [pivot.i18n :refer [t]]
            [pivot.dw :as dw]
            [pivot.lib.react :refer [rmap]]
            [pivot.rpc :as rpc]
            [pivot.cube-view.shared :refer [panel-header cube-view]]))

(defn- sort-results-according-to-selected-measures [result]
  (let [get-value-for-measure (fn [measure result]
                                (some #(when (= (:name measure) (name (first %)))
                                         (last %))
                                      result))]
    (->> (cube-view :measures)
         (map #(assoc % :value (get-value-for-measure % result))))))

(defn- totals-visualization []
  (let [result (-> (cube-view :results :main) first :result
                   sort-results-according-to-selected-measures)]
    [:div.ui.statistics
     (for [{:keys [name title value]} result]
       ^{:key name}
       [:div.statistic
        [:div.label title]
        [:div.value value]])]))

(defn visualization-panel []
  [:div.visualization.zone.panel.ui.basic.segment (rpc/loading-class [:results :main])
   (when (cube-view :results :main)
     (if (empty? (cube-view :measures))
       [:div.icon-hint
        [:i.warning.circle.icon]
        [:div.text (t :cubes/no-measures)]]
       (if (empty? (cube-view :split))
         [totals-visualization]
         [:div "TODO lista"])))])
