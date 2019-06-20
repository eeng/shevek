(ns shevek.pages.designer.visualizations.pivot-table
  (:require [reagent.core :as r]
            [shevek.reflow.core :refer [dispatch]]
            [shevek.reflow.db :as db]
            [shevek.i18n :refer [t]]
            [shevek.domain.dimension :refer [find-dimension]]
            [shevek.pages.designer.helpers :refer [current-cube]]
            [shevek.components.popup :refer [show-popup close-popup]]
            [shevek.components.virtualized :refer [virtual-table]]
            [shevek.domain.pivot-table :as pivot-table :refer [SplitsCell MeasureCell DimensionValueCell MeasureValueCell EmptyCell]]
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

(defn- sortable-th [title sorting-mapping html-opts {:keys [in-designer?]}]
  (if in-designer?
    (let [sorting-mapping (transform [MAP-VALS] translate-to-default-sort-by sorting-mapping)
          requested-sort-bys (map :name (vals sorting-mapping))
          current-sort-bys (map (comp :name :sort-by) (keys sorting-mapping))
          descendings (->> (keys sorting-mapping) (map (comp :descending :sort-by)) distinct)
          show-icon? (and (= current-sort-bys requested-sort-bys)
                          (= (count descendings) 1))
          icon-after? (= (:class html-opts) "right aligned")
          desc (first descendings)]
      [:th (assoc html-opts :on-click #(dispatch :designer/splits-sorted-by sorting-mapping (if show-icon? (not desc) true)))
       (when-not icon-after? [:span title])
       (when show-icon?
         [:i.icon.caret {:class (if desc "down" "up")}])
       (when icon-after? [:span title])])
    [:th html-opts title]))

(defprotocol ReagentCell
  (as-component [this context]))

(extend-protocol ReagentCell
  EmptyCell
  (as-component [cell _]
    [:th])

  SplitsCell
  (as-component [{:keys [dimensions in-columns col-span]} context]
    [sortable-th
     (->> dimensions (map :title) (str/join ", "))
     (zipmap dimensions dimensions)
     {:class (when in-columns "right aligned") :col-span (when (> col-span 1) col-span)}
     context])

  MeasureCell
  (as-component [{:keys [measure splits top-left-corner]} context]
    [sortable-th
     (:title measure)
     (zipmap splits (repeat measure))
     (when-not top-left-corner {:class "right aligned"})
     context])

  DimensionValueCell
  (as-component [{:keys [text depth in-columns col-span]} _]
    [(if in-columns :th.dim-value :td.dim-value)
     {:class (when in-columns "right aligned") :col-span (when (> col-span 1) col-span)}
     [:span {:class (str "depth-" depth)} text]])

  MeasureValueCell
  (as-component [{:keys [value text proportion]} _]
    (let [show-proportion? (> proportion 0.01)]
      [:td.right.aligned
       (when show-proportion? [proportion-bg value proportion])
       text])))

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

(defn- header-renderer [context {:keys [row-idx]}]
  (let [row (get-in context [:pivot-table :head row-idx])]
    (into [:tr]
          (map #(as-component % context) row))))

(defn- show-row-popup [event row-key slice selected]
  (let [tr (-> (.-target event) js/$ (.closest "tr"))
        relative-x-coord (- (.-pageX event) (-> tr .offset .-left))
        offset (- relative-x-coord (/ (.width tr) 2))]
    (show-popup event ^{:key row-key} [row-popup slice]
                {:position "top center"
                 :offset offset
                 :setFluidWidth true
                 :class "pivot-table-popup"
                 :on-toggle #(reset! selected %)})))

(defn- row-renderer []
  (let [selected (r/atom false)]
    (fn [{:keys [in-designer?] :as context} {:keys [row-idx]}]
      (let [row (get-in context [:pivot-table :body row-idx])
            {:keys [cells slice grand-total? subtotal?]} row]
        (into [:tr {:class [(when grand-total? "grand-total")
                            (when @selected "active")
                            (when subtotal? "subtotal")]
                    :on-click (when (and (not grand-total?) in-designer?)
                                #(show-row-popup % row-idx slice selected))}]
              (map #(as-component % context) cells))))))

(defn table-visualization [viz]
  (let [{:keys [head body] :as pt} (pivot-table/generate viz)
        context {:pivot-table pt
                 :in-designer? (some? (db/get :designer))}]
    [virtual-table
     {:class "pivot-table"
      :row-height 34
      :header-count (count head)
      :header-renderer (fn [{:keys [row-idx] :as props}]
                         ^{:key row-idx} [header-renderer context props])
      :row-count (count body)
      :row-renderer (fn [{:keys [row-idx] :as props}]
                      ^{:key row-idx} [row-renderer context props])}]))
