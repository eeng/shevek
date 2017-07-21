(ns shevek.viewer.visualization
  (:require-macros [shevek.reflow.macros :refer [defevh]])
  (:require [reagent.core :as r]
            [clojure.string :as str]
            [shevek.reflow.core :refer [dispatch]]
            [shevek.reflow.db :as db]
            [shevek.i18n :refer [t]]
            [shevek.lib.collections :refer [detect]]
            [shevek.navegation :refer [current-page?]]
            [shevek.rpc :as rpc]
            [shevek.viewer.shared :refer [panel-header format-measure format-dimension totals-result? dimension-value]]
            [shevek.components.drag-and-drop :refer [droppable]]
            [shevek.components.popup :refer [show-popup close-popup popup-opened?]]
            [shevek.viewer.filter :refer [build-filter]]))

(defn- sort-results-according-to-selected-measures [viewer]
  (let [result (first (get-in viewer [:results :main]))]
    (map #(assoc % :value (format-measure % result))
         (viewer :measures))))

(defn- totals-visualization [viewer]
  (let [result (sort-results-according-to-selected-measures viewer)]
    [:div.ui.statistics
     (for [{:keys [name title value]} result]
       [:div.statistic {:key name}
        [:div.label title]
        [:div.value value]])]))

(defn- calculate-rate [measure-value max-value]
  (let [rate (if (zero? max-value)
               0
               (/ measure-value max-value))]
    (str (* (Math/abs rate) 100) "%")))

(defn- row-popup [dim result filter-path]
  [:div
   [:div.dimension-value (format-dimension dim result)]
   [:div.buttons
    [:button.ui.primary.compact.button
     {:on-click #(dispatch :pivot-table-row-filtered filter-path "include")}
     (t :actions/select)]
    [:button.ui.compact.button
     {:on-click #(dispatch :pivot-table-row-filtered filter-path "exclude")}
     (t :cubes.operator/exclude)]
    [:button.ui.compact.button
     {:on-click #(do (close-popup)
                   (dispatch :viewer/raw-data-requested (build-filter dim {:operator "include" :value #{(dimension-value dim result)}})))}
     (t :raw-data/button)]
    [:button.ui.compact.button {:on-click close-popup} (t :actions/cancel)]]])

; TODO PERF cada vez q se clickea una row se renderizan todas las otras, ver de mejorar
(defn- pivot-table-row [result dim depth measures max-values value-result-path]
  (let [filter-path (map (fn [[d r]] [d (dimension-value d r)]) value-result-path)
        row-key (hash filter-path)
        totals-row (totals-result? result dim)]
    [:tr {:on-click #(when-not totals-row
                       (show-popup % ^{:key (hash result)} [row-popup dim result filter-path]
                                   {:position "top center" :distanceAway 135 :setFluidWidth true
                                    :class "pivot-table-popup" :id row-key}))
          :class (when (and (not totals-row) (popup-opened? row-key)) "active")}
     [:td
      [:div {:class (str "depth-" depth)} (format-dimension dim result)]]
     (for [measure measures
           :let [measure-name (-> measure :name keyword)
                 measure-value (measure-name result)]]
       [:td.right.aligned {:key measure-name}
        [:div.bg (when-not totals-row
                   {:class (when (neg? measure-value) "neg")
                    :style {:width (calculate-rate measure-value (max-values measure-name))}})]
        (format-measure measure result)])]))

(defn- pivot-table-rows
  ([results dims depth measures max-values]
   (pivot-table-rows results dims depth measures max-values []))
  ([results [dim & dims] depth measures max-values value-result-path]
   (when dim
     (mapcat (fn [result]
               (let [new-path (conj value-result-path [dim result])]
                 (into [(pivot-table-row result dim depth measures max-values new-path)]
                       (pivot-table-rows (:_results result) dims (inc depth) measures max-values new-path))))
             results))))

(defn- calculate-max-values [measures results]
  (reduce (fn [max-values measure-name]
            (assoc max-values measure-name (->> results rest (map measure-name) (apply max))))
          {}
          (map (comp keyword :name) measures)))

(defn- sortable-th [title on-click-sort-splits-by split opts]
  (if (current-page? :viewer)
    (let [on-click-sort-splits-by (map #(select-keys % [:name :title :type :expression]) on-click-sort-splits-by)
          sort-bys (map (comp :name :sort-by) split)
          descendings (->> split (map (comp :descending :sort-by)) distinct)
          show-icon? (and (= sort-bys (map :name on-click-sort-splits-by))
                          (= (count descendings) 1))
          icon-after? (= (:class opts) "right aligned")
          desc (first descendings)]
      [:th (assoc opts :on-click #(dispatch :splits-sorted-by on-click-sort-splits-by (if show-icon? (not desc) true)))
       (when-not icon-after? [:span title])
       (when show-icon?
         [:i.icon.caret {:class (if desc "down" "up")}])
       (when icon-after? [:span title])])
    [:th opts title]))

(defn- pivot-table-visualization [viewer]
  (let [split (viewer :arrived-split)
        measures (viewer :measures)
        results (get-in viewer [:results :main])
        max-values (calculate-max-values measures results)]
    [:table.ui.very.basic.compact.fixed.single.line.table.pivot-table
     [:thead>tr
      [sortable-th (->> split (map :title) (str/join ", ")) split split]
      (for [{:keys [name title] :as measure} measures]
        ^{:key name} [sortable-th title (repeat (count split) measure) split {:class "right aligned"}])]
     (into [:tbody] (pivot-table-rows results split 0 measures max-values))]))

(defn visualization [viewer]
  (when (get-in viewer [:results :main])
    [:div.visualization
     (if (empty? (viewer :measures))
       [:div.icon-hint
        [:i.warning.circle.icon]
        [:div.text (t :cubes/no-measures)]]
       (if (empty? (viewer :arrived-split))
         [totals-visualization viewer]
         [pivot-table-visualization viewer]))]))

(defn visualization-panel []
  [:div.visualization-container.zone.panel.ui.basic.segment
   (merge (droppable #(dispatch :split-replaced %))
          (rpc/loading-class [:results :main]))
   [visualization (db/get :viewer)]])
