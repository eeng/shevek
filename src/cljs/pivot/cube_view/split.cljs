(ns pivot.cube-view.split
  (:require-macros [pivot.lib.reagent :refer [rfor]]
                   [reflow.macros :refer [defevh]])
  (:require [reagent.core :as r]
            [reflow.core :refer [dispatch]]
            [pivot.i18n :refer [t]]
            [pivot.dw :refer [add-dimension remove-dimension dim=? time-dimension? replace-dimension]]
            [pivot.lib.react :refer [rmap]]
            [pivot.cube-view.shared :refer [panel-header current-cube cube-view send-main-query]]
            [pivot.cube-view.pinboard :refer [send-pinboard-queries]]
            [pivot.components :refer [with-controlled-popup select]]))

(defn- init-splitted-dim [dim {:keys [cube-view]}]
  (let [other-dims-in-split (remove #(dim=? % dim) (:split cube-view))]
    (cond-> (assoc dim
                   :limit (if (seq other-dims-in-split) 5 50)
                   :sort-by (-> cube-view :measures first :name)
                   :descending (not (time-dimension? dim)))
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

(defn- split-popup [selected dim]
  (let [opts (r/atom (select-keys dim [:limit :sort-by :descending]))
        close-popup #(reset! selected false)
        measures (current-cube :measures)]
    (fn []
      [:div.ui.special.popup {:style {:display (if @selected "block" "none")}}
       [:div.ui.form
        [:div.field
         [:label (t :cubes/sort-by)]
         [:div.flex.field
          [select (map (juxt :title :name) measures)
           {:class "fluid selection" :selected (:sort-by @opts) :on-change #(swap! opts assoc :sort-by %)}]
          [:button.ui.basic.icon.button
           {:on-click #(swap! opts update :descending not)}
           [:i.long.arrow.icon {:class (if (@opts :descending) "down" "up")}]]]]
        [:div.field
         [:label (t :cubes/limit)]
         [select (map (juxt identity identity) [5 10 25 50 100])
          {:selected (:limit @opts) :on-change #(swap! opts assoc :limit %)}]]
        [:button.ui.primary.button {:on-click #(do (close-popup)
                                                 (dispatch :split-options-changed dim @opts))} (t :answer/ok)]
        [:button.ui.button {:on-click close-popup} (t :answer/cancel)]]])))

(defn- split-item [selected {:keys [title] :as dim}]
  [:button.ui.orange.compact.right.labeled.icon.button
   {:on-click #(swap! selected not)}
   [:i.close.icon {:on-click #(dispatch :dimension-removed-from-split dim)}]
   title])

(defn split-panel []
  [:div.split.panel
   [panel-header (t :cubes/split)]
   (rmap (with-controlled-popup split-item split-popup {:position "bottom center"})
         (cube-view :split))])
