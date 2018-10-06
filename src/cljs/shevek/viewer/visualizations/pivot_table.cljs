(ns shevek.viewer.visualizations.pivot-table
  (:require [clojure.string :as str]
            [shevek.reflow.core :refer [dispatch] :refer-macros [defevh]]
            [shevek.i18n :refer [t]]
            [shevek.navigation :refer [current-page?]]
            [shevek.domain.dimension :refer [find-dimension]]
            [shevek.viewer.shared :refer [current-cube]]
            [shevek.components.popup :refer [show-popup close-popup popup-opened?]]
            [shevek.domain.pivot-table :as pivot-table :refer [SplitsCell MeasureCell DimensionValueCell MeasureValueCell EmptyCell]]
            [com.rpl.specter :refer [transform MAP-VALS]]))

(defn- proportion-bg [value proportion]
  (let [width (str proportion "%")]
    [:div.bg {:class (when (neg? value) "neg")
              :style {:width width}}]))

(defn- translate-to-default-sort-by [{:keys [default-sort-by] :as dim}]
  (if default-sort-by
    (find-dimension default-sort-by (current-cube :dimensions))
    dim))

(defn- sortable-th [title sorting-mapping opts]
  (if (current-page? :viewer)
    (let [sorting-mapping (transform [MAP-VALS] translate-to-default-sort-by sorting-mapping)
          requested-sort-bys (map :name (vals sorting-mapping))
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

(defprotocol ReagentCell
  (as-component [this]))

(extend-protocol ReagentCell
  EmptyCell
  (as-component [cell]
    [:th])

  SplitsCell
  (as-component [{:keys [dimensions in-columns col-span]}]
    [sortable-th
     (->> dimensions (map :title) (str/join ", "))
     (zipmap dimensions dimensions)
     {:class (when in-columns "right aligned") :col-span (when (> col-span 1) col-span)}])

  MeasureCell
  (as-component [{:keys [measure splits top-left-corner]}]
    [sortable-th
     (:title measure)
     (zipmap splits (repeat measure))
     (when-not top-left-corner {:class "right aligned"})])

  DimensionValueCell
  (as-component [{:keys [text depth in-columns col-span]}]
    [(if in-columns :th.dim-value :td.dim-value)
     {:class (when in-columns "right aligned") :col-span (when (> col-span 1) col-span)}
     [:span {:class (str "depth-" depth)} text]])

  MeasureValueCell
  (as-component [{:keys [value text proportion]}]
    [:td.right.aligned
     [proportion-bg value proportion]
     [:span text]]))

(defn- row-popup [slice]
  (let [simplified-slice (map (juxt :dimension :value) slice)]
    [:div
     [:div.dimension-value (str/join ", " (map :text slice))]
     [:div.buttons
      [:button.ui.primary.compact.button
       {:on-click #(dispatch :pivot-table-row-filtered simplified-slice "include")}
       (t :actions/select)]
      [:button.ui.compact.button
       {:on-click #(dispatch :pivot-table-row-filtered simplified-slice "exclude")}
       (t :viewer.operator/exclude)]
      [:button.ui.compact.button
       {:on-click #(do (close-popup) (dispatch :viewer/raw-data-requested simplified-slice))}
       (t :raw-data/button)]
      [:button.ui.compact.button {:on-click close-popup} (t :actions/cancel)]]]))

(defn- body-row [{:keys [cells slice grand-total?]}]
  (let [row-key (hash slice)]
    (into
     [:tr {:key row-key
           :class (if grand-total? "grand-total" (when (popup-opened? row-key) "active"))
           :on-click (when (and (not grand-total?) (current-page? :viewer))
                       #(show-popup % ^{:key row-key} [row-popup slice]
                                    {:position "top center" :distanceAway 135 :setFluidWidth true
                                     :class "pivot-table-popup" :id row-key}))}]
     (map as-component cells))))

(defn- head-row [row]
  (into [:tr] (map as-component row)))

(defn table-visualization [viz]
  (let [{:keys [head body]} (pivot-table/generate viz)]
    [:table.ui.very.basic.compact.table.pivot-table
     (into [:thead] (map head-row head))
     (into [:tbody] (map body-row body))]))
