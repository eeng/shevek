(ns pivot.cube-view.visualization
  (:require-macros [pivot.lib.reagent :refer [rfor]])
  (:require [reagent.core :as r]
            [clojure.string :as str]
            [pivot.i18n :refer [t]]
            [pivot.dw :as dw]
            [pivot.lib.react :refer [rmap]]
            [pivot.rpc :as rpc]
            [pivot.cube-view.shared :refer [panel-header cube-view format-measure format-dimension]]))

(defn- sort-results-according-to-selected-measures [result]
  (let [get-value-for-measure (fn [measure result]
                                (some #(when (= (:name measure) (name (first %)))
                                         (format-measure (last %) measure))
                                      result))]
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

; FIXME el :let es xq solo anda para una dim x ahora
; TODO refactorizar
(defn- pivot-table-visualization []
  (let [split (cube-view :split-arrived)]
    [:table.ui.very.basic.table
     [:thead
      [:tr
       [:th (->> split (map :title) (str/join ", "))]
       (rmap (fn [{:keys [title]}] [:th.right.aligned title]) (cube-view :measures))]]
     [:tbody
      (rfor [result (cube-view :results :main)
             :let [dimension (first split)
                   dimension-value (-> dimension :name keyword result)]]
        [:tr
         [:td (format-dimension dimension-value dimension)]
         (rfor [measure (cube-view :measures)
                :let [measure-value (-> measure :name keyword result)]]
           [:td.right.aligned (format-measure measure-value measure)])])]]))

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
