(ns shevek.viewer.visualizations.pivot-table
  (:require [clojure.string :as str]
            [shevek.reflow.core :refer [dispatch] :refer-macros [defevh]]
            [shevek.i18n :refer [t]]
            [shevek.navigation :refer [current-page?]]
            [shevek.rpc :as rpc]
            [shevek.viewer.shared :refer [panel-header format-measure format-dimension totals-result? dimension-value measure-value]]
            [shevek.components.popup :refer [show-popup close-popup popup-opened?]]
            [shevek.viewer.filter :refer [build-filter]]
            [shevek.lib.collections :refer [detect]]
            [shevek.lib.dw.dims :refer [partition-splits]]))

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

(defn- proportion-bg [measure-value max-value]
  (let [rate (if (zero? max-value) 0 (/ measure-value max-value))
        width (str (* (Math/abs rate) 100) "%")]
    [:div.bg {:class (when (neg? measure-value) "neg")
              :style {:width width}}]))

(defn- recursive-self-and-children [result grand-total [col-split & col-splits]]
  (let [corresponding (fn [result coll]
                        (or (detect #(= (dimension-value col-split %) (dimension-value col-split result)) coll)
                            {(keyword (:name col-split)) (dimension-value col-split result)}))
        completed-children (map #(corresponding % (:child-cols result)) (:child-cols grand-total))]
    (concat (mapcat #(recursive-self-and-children % (corresponding % (:child-cols grand-total)) col-splits)
                    completed-children)
            [result])))

(defn- table-row [result dim {:keys [measures max-values results col-splits]} value-result-path]
  (let [totals-row? (totals-result? result dim)
        simplified-path (map (fn [[{:keys [name]} value]] [(if totals-row? "grand-total" name) value]) value-result-path)
        row-key (hash simplified-path)
        depth (dec (count value-result-path))]
    (into
     [:tr {:on-click #(when (and (not totals-row?) (current-page? :viewer))
                        (show-popup % ^{:key (hash result)} [row-popup dim result value-result-path]
                                    {:position "top center" :distanceAway 135 :setFluidWidth true
                                     :class "pivot-table-popup" :id row-key}))
           :class (when (and (not totals-row?) (popup-opened? row-key)) "active")
           :key row-key}
      [:td
       [:div {:class (str "depth-" depth)} (format-dimension dim result)]]]
     (for [result (recursive-self-and-children result (first results) col-splits)
           measure measures
           :let [measure-name (-> measure :name keyword)
                 value (measure-value measure result)]]
       [:td.right.aligned {:title value}
        (when-not totals-row? [proportion-bg value (max-values measure-name)])
        (format-measure measure result)]))))

(defn- table-rows
  ([{:keys [results row-splits] :as viz}]
   (table-rows results row-splits viz []))
  ([results [dim & row-splits] viz value-result-path]
   (mapcat (fn [result]
             (let [new-path (conj value-result-path [dim (dimension-value dim result)])
                   parent-row (table-row result dim viz new-path)
                   child-rows (table-rows (:child-rows result) row-splits viz new-path)]
               (into [parent-row] child-rows)))
           results)))

(defn- calculate-max-values [measures results]
  (reduce (fn [max-values measure-name]
            (assoc max-values measure-name (->> results rest (map measure-name) (apply max))))
          {}
          (map (comp keyword :name) measures)))

(defn- self-and-children [{:keys [child-cols] :as result}]
  (concat child-cols [result]))

(defn- calculate-col-span [{:keys [grand-total?] :as result} col-splits measures]
  (if (or grand-total? (empty? col-splits))
    (count measures)
    (->> (self-and-children result)
         (map #(calculate-col-span % (rest col-splits) measures))
         (reduce +))))

(defn- sortable-th [title sorting-mapping opts]
  (if (current-page? :viewer)
    (let [requested-sort-bys (map :name (vals sorting-mapping))
          current-sort-bys (map (comp :name :sort-by) (keys sorting-mapping))
          descendings (->> (keys sorting-mapping) (map (comp :descending :sort-by)) distinct)
          show-icon? (and (= current-sort-bys requested-sort-bys)
                          (= (count descendings) 1))
          icon-after? (= (:class opts) "right aligned")
          desc (first descendings)]
      [:th (assoc opts :on-click #(dispatch :splits-sorted-by sorting-mapping (if show-icon? (not desc) true)))
       (when-not icon-after? [:span title])
       (when show-icon?
         [:i.icon.caret {:class (if desc "down" "up")}])
       (when icon-after? [:span title])])
    [:th opts title]))

(defn- row-splits-header [{:keys [row-splits]}]
  [sortable-th (->> row-splits (map :title) (str/join ", ")) (zipmap row-splits row-splits)])

(defn- col-split-header [{:keys [title] :as dim} opts]
  [sortable-th title {dim dim} opts])

(defn- measure-header [{:keys [title] :as measure} {:keys [splits]} opts]
  [sortable-th title (zipmap splits (repeat measure)) opts])

(defn- measure-headers [{:keys [measures] :as viz} results]
  (into
   [:tr [row-splits-header viz]]
   (for [result results measure measures]
     [measure-header measure viz {:class "right aligned"}])))

(defn- multiple-measures-layout? [{:keys [measures col-splits]}]
  (or (> (count measures) 1) (empty? col-splits)))

(defn- col-split-headers [dim next-col-splits results
                          {:keys [measures col-splits row-splits] :as viz}]
  (let [results (map #(assoc % :col-span (calculate-col-span % next-col-splits measures)) results)
        full-col-span (->> results (map :col-span results) (reduce +))
        dim-values (for [result results :let [col-span (:col-span result)]]
                    [:th.right.aligned.dim-value {:col-span (when-not (= 1 col-span) col-span)}
                     (format-dimension dim result)])]
    (if (multiple-measures-layout? viz)
      [(into [:tr [col-split-header dim]] dim-values)]
      [[:tr
        (if (= dim (first col-splits)) [measure-header (first measures) viz] [:th])
        [col-split-header dim {:col-span full-col-span :class "right aligned"}]]
       (into [:tr (if (= dim (last col-splits)) [row-splits-header viz] [:th])] dim-values)])))

(defn- table-headers [[dim & next-col-splits] results rows viz]
  (if dim
    (let [new-rows (col-split-headers dim next-col-splits results viz)
          next-results (mapcat #(if (or (:grand-total? %) (empty? next-col-splits)) [%] (self-and-children %)) results)]
      (table-headers next-col-splits next-results (into rows new-rows) viz))
    (if (multiple-measures-layout? viz)
      (conj rows (measure-headers viz results))
      rows)))

(defn table-visualization [{:keys [measures splits results] :as viz}]
  (let [max-values (calculate-max-values measures results)
        [row-splits col-splits] (partition-splits splits)
        viz (assoc viz :max-values max-values :row-splits row-splits :col-splits col-splits)]
    [:table.ui.very.basic.compact.fixed.single.line.table.pivot-table
     (into [:thead] (table-headers col-splits (self-and-children (assoc (first results) :grand-total? true)) [] viz))
     [:tbody (doall (table-rows viz))]]))
