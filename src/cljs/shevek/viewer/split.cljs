(ns shevek.viewer.split
  (:require [reagent.core :as r]
            [shevek.reflow.core :refer [dispatch] :refer-macros [defevh defevhi]]
            [shevek.i18n :refer [t]]
            [shevek.domain.dimension :refer [add-dimension remove-dimension dim= time-dimension? replace-dimension find-dimension clean-dim row-split?]]
            [shevek.lib.time.ext :refer [default-granularity]]
            [shevek.lib.react :refer [without-propagation]]
            [shevek.lib.collections :refer [includes?]]
            [shevek.viewer.shared :refer [panel-header current-cube viewer send-main-query]]
            [shevek.components.popup :refer [show-popup close-popup]]
            [shevek.components.form :refer [select]]
            [shevek.components.drag-and-drop :refer [draggable droppable]]
            [shevek.viewer.url :refer [store-viewer-in-url]]
            [cuerdas.core :as str]
            [com.rpl.specter :refer [transform ALL]]))

(defn- init-splitted-dim [{:keys [limit sort-by granularity default-sort-by] :as dim} {:keys [viewer]}]
  (let [first-measure (or (-> viewer :measures first)
                          (-> viewer :cube :measures first))
        default-sort-by (find-dimension default-sort-by (-> viewer :cube :dimensions))
        initial-sort-by (or sort-by
                            (and default-sort-by (assoc default-sort-by :descending false))
                            (assoc first-measure :descending (not (time-dimension? dim))))]
    (cond-> (assoc (clean-dim dim)
                   :on "rows"
                   :limit (or limit (and (time-dimension? dim) 1000) 50)
                   :sort-by initial-sort-by)
            (time-dimension? dim) (assoc :granularity (or granularity (default-granularity viewer))))))

(defn- adjust-viztype [{:keys [viewer] :as db}]
  (let [{old-viztype :viztype splits :splits} viewer
        new-viztype (cond
                      (and (= old-viztype :totals) (seq splits)) :table
                      (empty? splits) :totals
                      :else old-viztype)]
    (assoc-in db [:viewer :viztype] new-viztype)))

(defevhi :split-dimension-added [{:keys [viewer] :as db} dim]
  {:after [store-viewer-in-url]}
  (let [limit (when (->> viewer :splits (filter row-split?) seq) 5)]
    (-> (update-in db [:viewer :splits] add-dimension (init-splitted-dim (assoc dim :limit limit) db))
        adjust-viztype
        send-main-query)))

(defevhi :split-replaced [db dim]
  {:after [close-popup store-viewer-in-url]}
  (-> (assoc-in db [:viewer :splits] [(init-splitted-dim dim db)])
      adjust-viztype
      send-main-query))

(defevhi :split-dimension-replaced [db old-dim new-dim]
  {:after [close-popup store-viewer-in-url]}
  (-> (update-in db [:viewer :splits] replace-dimension old-dim (init-splitted-dim new-dim db))
      send-main-query))

(defevhi :split-dimension-removed [db dim]
  {:after [close-popup store-viewer-in-url]}
  (-> (update-in db [:viewer :splits] remove-dimension dim)
      adjust-viztype
      send-main-query))

(defevhi :split-options-changed [db dim opts]
  {:after [close-popup store-viewer-in-url]}
  (-> (update-in db [:viewer :splits] replace-dimension (merge dim opts))
      send-main-query))

(defevhi :splits-sorted-by [db sort-bys descending]
  {:after [store-viewer-in-url]}
  (-> (transform [:viewer :splits ALL (partial includes? (keys sort-bys))]
                 #(assoc % :sort-by (-> (sort-bys %)
                                        (select-keys [:name :title :type :expression])
                                        (assoc :descending descending)))
                 db)
      (send-main-query)))

(def granularities {"PT5M" "5m", "PT1H" "1H", "P1D" "1D", "P1W" "1W", "P1M" "1M"})

(defn- split-popup [{:keys [default-sort-by] :as dim}]
  (let [opts (r/atom (select-keys dim [:on :limit :sort-by :granularity]))
        posible-sort-bys (cond-> (conj (current-cube :measures) (clean-dim dim))
                                 default-sort-by (conj (find-dimension default-sort-by (current-cube :dimensions))))]
    (fn [dim]
      (let [desc (get-in @opts [:sort-by :descending])
            {:keys [granularity on]} @opts]
        [:div.split.popup
         [:div.ui.form
          [:div.field
           [:label (t :viewer/split-on)]
           [:div.ui.two.small.basic.buttons
            (for [[split-on title] [["rows" (t :viewer/rows)] ["columns" (t :viewer/columns)]]]
              [:button.ui.button {:key split-on
                                  :class (when (= on split-on) "active")
                                  :on-click #(swap! opts assoc :on split-on)}
               title])]]
          (when (time-dimension? dim)
            [:div.field.periods
             [:label (t :viewer/granularity)]
             [:div.ui.five.small.basic.buttons
              (for [[period title] granularities]
                [:button.ui.button {:key period
                                    :class (when (= granularity period) "active")
                                    :on-click #(swap! opts assoc :granularity period)}
                 title])]])
          [:div.field
           [:label (t :viewer/sort-by)]
           [:div.flex.field
            [select (map (juxt :title :name) posible-sort-bys)
             {:class "fluid selection" :selected (get-in @opts [:sort-by :name])
              :on-change #(swap! opts assoc :sort-by (assoc (find-dimension % posible-sort-bys)
                                                            :descending desc))}]
            [:button.ui.basic.icon.button
             {:on-click #(swap! opts update-in [:sort-by :descending] not)}
             [:i.long.arrow.icon {:class (if desc "down" "up")}]]]]
          [:div.field
           [:label (t :viewer/limit)]
           [select (map (juxt identity identity) [5 10 25 50 100 1000])
            {:selected (:limit @opts) :on-change #(swap! opts assoc :limit (str/parse-int %))}]]
          [:button.ui.primary.compact.button
           {:on-click #(dispatch :split-options-changed dim @opts)}
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
                (droppable #(dispatch :split-dimension-replaced dim %)))
         [:i.close.icon {:on-click (without-propagation dispatch :split-dimension-removed dim)}]
         title]))))

(defn split-panel []
  [:div.split.panel (droppable #(dispatch :split-dimension-added %))
   [panel-header (t :viewer/splits)]
   (for [dim (viewer :splits)]
     ^{:key (:name dim)} [split-item dim])])
