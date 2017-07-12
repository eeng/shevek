(ns shevek.viewer.split
  (:require-macros [reflow.macros :refer [defevh]])
  (:require [reagent.core :as r]
            [reflow.core :refer [dispatch]]
            [shevek.i18n :refer [t]]
            [shevek.lib.dw.dims :refer [add-dimension remove-dimension dim= time-dimension? replace-dimension find-dimension clean-dim]]
            [shevek.lib.dw.time :refer [default-granularity]]
            [shevek.lib.react :refer [rmap without-propagation]]
            [shevek.viewer.shared :refer [panel-header current-cube viewer send-main-query]]
            [shevek.components.popup :refer [controlled-popup]]
            [shevek.components.form :refer [select]]
            [shevek.components.drag-and-drop :refer [draggable droppable]]
            [cuerdas.core :as str]))

(defn- init-splitted-dim [{:keys [limit sort-by granularity] :as dim} {:keys [viewer]}]
  (cond-> (assoc (clean-dim dim)
                 :limit (or limit 50)
                 :sort-by (or sort-by (assoc (-> viewer :measures first) :descending (not (time-dimension? dim)))))
          (time-dimension? dim) (assoc :granularity (or granularity (default-granularity viewer)))))

(defevh :splid-dimension-added [{:keys [viewer] :as db} dim]
  (let [limit (when (seq (:split viewer)) 5)]
    (-> (update-in db [:viewer :split] add-dimension (init-splitted-dim (assoc dim :limit limit) db))
        (send-main-query))))

(defevh :split-replaced [db dim]
  (-> (assoc-in db [:viewer :split] [(init-splitted-dim dim db)])
      (send-main-query)))

(defevh :split-dimension-replaced [db old-dim new-dim]
  (-> (update-in db [:viewer :split] replace-dimension old-dim (init-splitted-dim new-dim db))
      (send-main-query)))

(defevh :split-dimension-removed [db dim]
  (-> (update-in db [:viewer :split] remove-dimension dim)
      (send-main-query)))

(defevh :split-options-changed [db dim opts]
  (-> (update-in db [:viewer :split] replace-dimension (merge dim opts))
      (send-main-query)))

(defevh :splits-sorted-by [db sort-bys descending]
  (-> (update-in db [:viewer :split]
                 (fn [splits]
                   (->> (zipmap splits sort-bys)
                        (mapv (fn [[split sort-by]]
                                (assoc split :sort-by (assoc sort-by :descending descending)))))))
      (send-main-query)))

(def granularities {"PT5M" "5m", "PT1H" "1H", "P1D" "1D", "P1W" "1W", "P1M" "1M"})

(defn- split-popup [_ dim]
  (let [opts (r/atom (select-keys dim [:limit :sort-by :granularity]))
        posible-sort-bys (conj (current-cube :measures) (clean-dim dim))]
    (fn [{:keys [close]} dim]
      (let [desc (get-in @opts [:sort-by :descending])
            current-granularity (@opts :granularity)]
        [:div.split.popup
         [:div.ui.form
          (when (time-dimension? dim)
            [:div.field.periods
             [:label (t :cubes/granularity)]
             [:div.ui.five.small.basic.buttons
              (for [[period title] granularities]
                [:button.ui.button {:key period
                                    :class (when (= current-granularity period) "active")
                                    :on-click #(swap! opts assoc :granularity period)}
                 title])]])
          [:div.field
           [:label (t :cubes/sort-by)]
           [:div.flex.field
            [select (map (juxt :title :name) posible-sort-bys)
             {:class "fluid selection" :selected (get-in @opts [:sort-by :name])
              :on-change #(swap! opts assoc :sort-by (assoc (find-dimension % posible-sort-bys)
                                                            :descending desc))}]
            [:button.ui.basic.icon.button
             {:on-click #(swap! opts update-in [:sort-by :descending] not)}
             [:i.long.arrow.icon {:class (if desc "down" "up")}]]]]
          [:div.field
           [:label (t :cubes/limit)]
           [select (map (juxt identity identity) [5 10 25 50 100])
            {:selected (:limit @opts) :on-change #(swap! opts assoc :limit (str/parse-int %))}]]
          [:button.ui.primary.compact.button
           {:on-click #(do (close) (dispatch :split-options-changed dim @opts))}
           (t :actions/ok)]
          [:button.ui.compact.button {:on-click close} (t :actions/cancel)]]]))))

(defn- split-item [{:keys [toggle]} {:keys [title] :as dim}]
  [:button.ui.orange.compact.right.labeled.icon.button
   (merge {:on-click toggle}
          (draggable dim)
          (droppable #(dispatch :split-dimension-replaced dim %)))
   [:i.close.icon {:on-click (without-propagation dispatch :split-dimension-removed dim)}]
   title])

(defn split-panel []
  [:div.split.panel (droppable #(dispatch :splid-dimension-added %))
   [panel-header (t :cubes/split)]
   (rmap (controlled-popup split-item split-popup {:position "bottom center"})
         (viewer :split)
         :name)])
