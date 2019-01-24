(ns shevek.pages.designer.splits
  (:require [reagent.core :as r]
            [shevek.reflow.core :refer [dispatch] :refer-macros [defevh defevhi]]
            [shevek.i18n :refer [t]]
            [shevek.domain.dimension :refer [add-dimension remove-dimension dim= time-dimension? replace-dimension find-dimension clean-dim row-split?]]
            [shevek.lib.time.ext :refer [default-granularity]]
            [shevek.lib.react :refer [without-propagation]]
            [shevek.lib.collections :refer [includes?]]
            [shevek.pages.designer.helpers :refer [panel-header current-cube send-designer-query]]
            [shevek.components.popup :refer [show-popup close-popup]]
            [shevek.components.form :refer [select]]
            [shevek.components.drag-and-drop :refer [draggable droppable]]
            [shevek.schemas.conversion :refer [clean-sort-by]]
            [cuerdas.core :as str]
            [com.rpl.specter :refer [transform ALL]]))

(defn- init-splitted-dim [{:keys [on limit sort-by granularity default-sort-by] :or {on "rows"} :as dim}
                          {:keys [designer]}]
  (let [first-measure (or (-> designer :measures first)
                          (-> designer :cube :measures first))
        default-sort-by (find-dimension default-sort-by (-> designer :cube :dimensions))
        initial-sort-by (or sort-by
                            (and default-sort-by (assoc default-sort-by :descending false))
                            (and (time-dimension? dim) (assoc dim :descending false))
                            (assoc first-measure :descending true))]
    (cond-> (assoc (clean-dim dim)
                   :on on
                   :limit (or limit (and (time-dimension? dim) 1000) 50)
                   :sort-by (clean-sort-by initial-sort-by))
            (time-dimension? dim) (assoc :granularity (or granularity (default-granularity designer))))))

(defn- adjust-viztype [{:keys [designer] :as db}]
  (let [{old-viztype :viztype splits :splits} designer
        new-viztype (cond
                      (and (= old-viztype :totals) (seq splits)) :table
                      (empty? splits) :totals
                      :else old-viztype)]
    (assoc-in db [:designer :viztype] new-viztype)))

(defevh :designer/split-dimension-added [{:keys [designer] :as db} dim]
  (let [limit (when (->> designer :splits (filter row-split?) seq) 5)]
    (-> (update-in db [:designer :splits] add-dimension (init-splitted-dim (assoc dim :limit limit) db))
        adjust-viztype
        send-designer-query)))

(defevhi :designer/split-replaced [db dim]
  {:after [close-popup]}
  (-> (assoc-in db [:designer :splits] [(init-splitted-dim dim db)])
      adjust-viztype
      send-designer-query))

(defevhi :designer/split-dimension-replaced [db old-dim new-dim]
  {:after [close-popup]}
  (-> (update-in db [:designer :splits] replace-dimension old-dim (init-splitted-dim new-dim db))
      send-designer-query))

(defevhi :designer/split-dimension-removed [db dim]
  {:after [close-popup]}
  (-> (update-in db [:designer :splits] remove-dimension dim)
      adjust-viztype
      send-designer-query))

(defevhi :designer/split-options-changed [db dim opts]
  {:after [close-popup]}
  (-> (update-in db [:designer :splits] replace-dimension (merge dim opts))
      send-designer-query))

(defevh :designer/splits-sorted-by [db sort-bys descending]
  (-> (transform [:designer :splits ALL (partial includes? (keys sort-bys))]
                 #(assoc % :sort-by (-> (sort-bys %)
                                        (assoc :descending descending)
                                        clean-sort-by))
                 db)
      send-designer-query))

(def granularities {"PT5M" "5m", "PT1H" "1H", "P1D" "1D", "P1W" "1W", "P1M" "1M"})

(defn- split-popup [{:keys [default-sort-by name] :as dim}]
  (let [opts (r/atom (select-keys dim [:on :limit :sort-by :granularity]))
        posible-sort-bys (cond-> (conj (current-cube :measures) dim)
                                 (and default-sort-by (not= default-sort-by name)) (conj (find-dimension default-sort-by (current-cube :dimensions))))]
    (fn [dim]
      (let [desc (get-in @opts [:sort-by :descending])
            {:keys [granularity on]} @opts]
        [:div.split.popup
         [:div.ui.form
          [:div.field
           [:label (t :designer/split-on)]
           [:div.ui.two.small.basic.buttons
            (for [[split-on title] [["rows" (t :designer/rows)] ["columns" (t :designer/columns)]]]
              [:button.ui.button {:key split-on
                                  :class (when (= on split-on) "active")
                                  :on-click #(swap! opts assoc :on split-on)}
               title])]]
          (when (time-dimension? dim)
            [:div.field.periods
             [:label (t :designer/granularity)]
             [:div.ui.five.small.basic.buttons
              (for [[period title] granularities]
                [:button.ui.button {:key period
                                    :class (when (= granularity period) "active")
                                    :on-click #(swap! opts assoc :granularity period)}
                 title])]])
          [:div.field
           [:label (t :designer/sort-by)]
           [:div.flex.field
            [select (map (juxt :title :name) posible-sort-bys)
             {:class "fluid selection" :selected (get-in @opts [:sort-by :name])
              :on-change #(swap! opts assoc :sort-by {:name % :descending desc})}]
            [:button.ui.basic.icon.button
             {:on-click #(swap! opts update-in [:sort-by :descending] not)}
             [:i.long.arrow.icon {:class (if desc "down" "up")}]]]]
          [:div.field
           [:label (t :designer/limit)]
           [select (map (juxt identity identity) [5 10 25 50 100 1000])
            {:selected (:limit @opts) :on-change #(swap! opts assoc :limit (str/parse-int %))}]]
          [:button.ui.primary.compact.button
           {:on-click #(dispatch :designer/split-options-changed dim @opts)}
           (t :actions/ok)]
          [:button.ui.compact.button {:on-click close-popup} (t :actions/cancel)]]]))))

; The popup-key needs two things:
; 1. The splitted dim so if we sort from the visualization the popup gets remounted with the updated opts.
; 2. A timestamp for if a split is removed and then re-added, the popup gets regenerated with default opts.
; The button has to be a link otherwise Firefox wouldn't fire the click event on the icon
(defn- split-item [_]
  (let [timestamp (js/Date.)]
    (fn [{:keys [title on] :as dim}]
      (let [popup-key (-> dim (assoc :timestamp timestamp) hash)]
        [:a.ui.compact.right.labeled.icon.button
         (merge {:on-click #(show-popup % ^{:key popup-key} [split-popup dim] {:position "bottom center"})
                 :class (if (= on "columns") "purple" "orange")}
                (draggable dim)
                (droppable #(dispatch :designer/split-dimension-replaced dim %)))
         [:i.close.icon {:on-click (without-propagation dispatch :designer/split-dimension-removed dim)}]
         title]))))

(defn splits-panel [{:keys [splits]}]
  [:div.split.panel (droppable #(dispatch :designer/split-dimension-added %))
   [panel-header (t :designer/splits)]
   (for [dim splits]
     ^{:key (:name dim)} [split-item dim])])
