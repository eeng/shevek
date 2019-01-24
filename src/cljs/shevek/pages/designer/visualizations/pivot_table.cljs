(ns shevek.pages.designer.visualizations.pivot-table
  (:require [reagent.core :as r]
            [shevek.reflow.core :refer [dispatch]]
            [shevek.reflow.db :as db]
            [shevek.i18n :refer [t]]
            [shevek.navigation :refer [current-page?]]
            [shevek.domain.dimension :refer [find-dimension]]
            [shevek.pages.designer.helpers :refer [current-cube]]
            [shevek.components.popup :refer [show-popup close-popup]]
            [shevek.domain.pivot-table :as pivot-table :refer [SplitsCell MeasureCell DimensionValueCell MeasureValueCell EmptyCell]]
            [shevek.lib.number :as number]
            [clojure.string :as str]
            [com.rpl.specter :refer [transform MAP-VALS]]))

(defn- proportion-bg [value proportion]
  (let [width (str (* proportion 0.98 100) "%")] ; Multiply by a fraction so there is some padding between measures
    [:div.bg {:class (when (neg? value) "neg")
              :style {:width width}}]))

(defn- translate-to-default-sort-by [{:keys [default-sort-by] :as dim}]
  (if default-sort-by
    (find-dimension default-sort-by (current-cube :dimensions))
    dim))

(defn- designer-visible? []
  (or (current-page? :designer)
      (db/get-in [:selected-panel :edit])))

(defn- sortable-th [title sorting-mapping opts]
  (if (designer-visible?)
    (let [sorting-mapping (transform [MAP-VALS] translate-to-default-sort-by sorting-mapping)
          requested-sort-bys (map :name (vals sorting-mapping))
          current-sort-bys (map (comp :name :sort-by) (keys sorting-mapping))
          descendings (->> (keys sorting-mapping) (map (comp :descending :sort-by)) distinct)
          show-icon? (and (= current-sort-bys requested-sort-bys)
                          (= (count descendings) 1))
          icon-after? (= (:class opts) "right aligned")
          desc (first descendings)]
      [:th (assoc opts :on-click #(dispatch :designer/splits-sorted-by sorting-mapping (if show-icon? (not desc) true)))
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
  (as-component [{:keys [value text proportion participation]}]
    (let [show-proportion? (> proportion 0.01)]
      [:td.right.aligned
       (when show-proportion? [proportion-bg value proportion])
       [:span (when show-proportion? {:title (number/format participation "0.0%")})
        text]])))

(defn- row-popup [slice]
  (let [simplified-slice (map (juxt :dimension :value) slice)]
    [:div
     [:div.dimension-value (str/join ", " (map :text slice))]
     [:div.buttons
      [:button.ui.primary.compact.button
       {:on-click #(dispatch :designer/pivot-table-row-filtered simplified-slice "include")}
       (t :actions/select)]
      [:button.ui.compact.button
       {:on-click #(dispatch :designer/pivot-table-row-filtered simplified-slice "exclude")}
       (t :designer.operator/exclude)]
      [:button.ui.compact.button
       {:on-click #(do (close-popup) (dispatch :designer/raw-data-requested simplified-slice))}
       (t :raw-data/button)]
      [:button.ui.compact.button {:on-click close-popup} (t :actions/cancel)]]]))

; The slice can't be used directly for the hash because it fails when the values are floats, as they are hash as ints and can produce duplicate values
(defn- body-row []
  (let [selected (r/atom false)]
    (fn [{:keys [cells slice grand-total?]} row-key]
      (into
       [:tr {:class (if grand-total? "grand-total" (when @selected "active"))
             :on-click (when (and (not grand-total?) (designer-visible?))
                         (fn [event]
                           (let [tr (-> (.-target event) js/$ (.closest "tr"))
                                 relative-x-coord (- (.-pageX event) (-> tr .offset .-left))
                                 offset (- relative-x-coord (/ (.width tr) 2))]
                             (show-popup event ^{:key row-key} [row-popup slice]
                                         {:position "top center"
                                          :offset offset
                                          :setFluidWidth true
                                          :class "pivot-table-popup"
                                          :on-toggle #(reset! selected %)}))))}]
       (map as-component cells)))))

(defn- head-row [row]
  (into [:tr] (map as-component row)))

(defn table-visualization [viz]
  (let [{:keys [head body]} (pivot-table/generate viz)]
    [:table.ui.very.basic.compact.table.pivot-table
     (into [:thead]
           (for [row head] [head-row row]))
     (into [:tbody]
           (for [[i row] (map-indexed vector body)]
             ^{:key i} [body-row row i]))]))
