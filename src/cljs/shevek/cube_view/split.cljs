(ns shevek.cube-view.split
  (:require-macros [reflow.macros :refer [defevh]])
  (:require [reagent.core :as r]
            [reflow.core :refer [dispatch]]
            [shevek.i18n :refer [t]]
            [shevek.dw :refer [add-dimension remove-dimension dim=? time-dimension? replace-dimension find-dimension]]
            [shevek.lib.react :refer [rmap without-propagation]]
            [shevek.cube-view.shared :refer [panel-header current-cube cube-view send-main-query]]
            [shevek.cube-view.pinboard :refer [send-pinboard-queries]]
            [shevek.components :refer [controlled-popup select]]))

; TODO el limit distinto no funca bien cuando se reemplaza el filter
; TODO permitir elegir otras granularidades en el popup
(defn- init-splitted-dim [dim {:keys [cube-view]}]
  (let [other-dims-in-split (remove #(dim=? % dim) (:split cube-view))]
    (cond-> (assoc dim
                   :limit (if (seq other-dims-in-split) 5 50)
                   :sort-by (assoc (-> cube-view :measures first)
                                   :descending (not (time-dimension? dim))))
            (time-dimension? dim) (assoc :granularity "PT1H"))))

(defevh :dimension-added-to-split [db dim]
  (-> (update-in db [:cube-view :split] add-dimension (init-splitted-dim dim db))
      (send-main-query)))

(defevh :dimension-replaced-split [db dim]
  (-> (assoc-in db [:cube-view :split] [(init-splitted-dim dim db)])
      (send-main-query)))

(defevh :dimension-removed-from-split [db dim]
  (-> (update-in db [:cube-view :split] remove-dimension dim)
      (send-main-query)))

(defevh :split-options-changed [db dim opts]
  (-> (update-in db [:cube-view :split] replace-dimension (merge dim opts))
      (send-main-query)))

(defevh :splits-sorted-by [db sort-bys descending]
  (-> (update-in db [:cube-view :split]
                 (fn [splits]
                   (->> (zipmap splits sort-bys)
                        (mapv (fn [[split sort-by]]
                                (assoc split :sort-by (assoc sort-by :descending descending)))))))
      (send-main-query)))

(defn- split-popup [_ dim]
  (let [opts (r/atom (select-keys dim [:limit :sort-by]))
        posible-sort-bys (conj (current-cube :measures) dim)]
    (fn [{:keys [close]} dim]
      (let [desc (get-in @opts [:sort-by :descending])]
        [:div
         [:div.ui.form
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
            {:selected (:limit @opts) :on-change #(swap! opts assoc :limit %)}]]
          [:button.ui.primary.compact.button
           {:on-click #(do (close) (dispatch :split-options-changed dim @opts))}
           (t :actions/ok)]
          [:button.ui.compact.button {:on-click close} (t :actions/cancel)]]]))))

(defn- split-item [{:keys [toggle]} {:keys [title] :as dim}]
  [:button.ui.orange.compact.right.labeled.icon.button {:on-click toggle}
   [:i.close.icon {:on-click (without-propagation dispatch :dimension-removed-from-split dim)}]
   title])

(defn split-panel []
  [:div.split.panel
   [panel-header (t :cubes/split)]
   (rmap (controlled-popup split-item split-popup {:position "bottom center"})
         (cube-view :split)
         :name)])
