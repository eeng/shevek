(ns shevek.viewer.visualizations.pivot-table
  (:require [clojure.string :as str]
            [shevek.reflow.core :refer [dispatch]]
            [shevek.i18n :refer [t]]
            [shevek.navegation :refer [current-page?]]
            [shevek.rpc :as rpc]
            [shevek.viewer.shared :refer [panel-header format-measure format-dimension totals-result? dimension-value]]
            [shevek.components.popup :refer [show-popup close-popup popup-opened?]]
            [shevek.viewer.filter :refer [build-filter]]))

(defn- calculate-rate [measure-value max-value]
  (let [rate (if (zero? max-value)
               0
               (/ measure-value max-value))]
    (str (* (Math/abs rate) 100) "%")))

(defn- row-popup [dim result selected-path]
  [:div
   [:div.dimension-value (format-dimension dim result)]
   [:div.buttons
    [:button.ui.primary.compact.button
     {:on-click #(dispatch :pivot-table-row-filtered selected-path "include")}
     (t :actions/select)]
    [:button.ui.compact.button
     {:on-click #(dispatch :pivot-table-row-filtered selected-path "exclude")}
     (t :viewer.operator/exclude)]
    [:button.ui.compact.button
     {:on-click #(do (close-popup) (dispatch :viewer/raw-data-requested selected-path))}
     (t :raw-data/button)]
    [:button.ui.compact.button {:on-click close-popup} (t :actions/cancel)]]])

; TODO PERF cada vez q se clickea una row se renderizan todas las otras, ver de mejorar
(defn- table-row [result dim depth measures max-values value-result-path]
  (let [selected-path (map (fn [[d r]] [d (dimension-value d r)]) value-result-path)
        row-key (hash selected-path)
        totals-row (totals-result? result dim)]
    [:tr {:on-click #(when-not totals-row
                       (show-popup % ^{:key (hash result)} [row-popup dim result selected-path]
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

(defn- table-rows
  ([results dims depth measures max-values]
   (table-rows results dims depth measures max-values []))
  ([results [dim & dims] depth measures max-values value-result-path]
   (when dim
     (mapcat (fn [result]
               (let [new-path (conj value-result-path [dim result])]
                 (into [(table-row result dim depth measures max-values new-path)]
                       (table-rows (:_results result) dims (inc depth) measures max-values new-path))))
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

(defn table-visualization [{:keys [measures split results]}]
  (let [max-values (calculate-max-values measures results)]
    [:table.ui.very.basic.compact.fixed.single.line.table.pivot-table
     [:thead>tr
      [sortable-th (->> split (map :title) (str/join ", ")) split split]
      (for [{:keys [name title] :as measure} measures]
        ^{:key name} [sortable-th title (repeat (count split) measure) split {:class "right aligned"}])]
     (into [:tbody] (table-rows results split 0 measures max-values))]))
