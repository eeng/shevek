(ns pivot.cube-view.split
  (:require-macros [pivot.lib.reagent :refer [rfor]]
                   [reflow.macros :refer [defevh]])
  (:require [reagent.core :as r]
            [reflow.core :refer [dispatch]]
            [pivot.i18n :refer [t]]
            [pivot.dw :refer [add-dimension remove-dimension dim=? time-dimension?]]
            [pivot.lib.react :refer [rmap]]
            [pivot.lib.collections :refer [replace-when]]
            [pivot.cube-view.shared :refer [panel-header cube-view send-main-query]]
            [pivot.cube-view.pinboard :refer [send-pinboard-queries]]
            [pivot.components :refer [with-controlled-popup select]]))

(defn- init-splitted-dim [dim {:keys [cube-view]}]
  (let [other-dims-in-split (remove #(dim=? % dim) (:split cube-view))]
    (cond-> (assoc dim :limit (if (seq other-dims-in-split) 5 50))
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

(defevh :split-options-changed [db dim {:keys [limit]}]
  (-> (update-in db [:cube-view :split]
                 (fn [dims] (replace-when (partial dim=? dim) #(assoc % :limit limit) dims)))
      (send-main-query)))

(defn- split-popup [selected {:keys [limit] :as dim}]
  (let [form-state (r/atom {:limit limit})
        close-popup #(reset! selected false)]
    (fn []
      [:div.ui.special.popup {:style {:display (if @selected "block" "none")}}
       [:div.ui.form
        [:div.field
         [:label (t :cubes/limit)]
         [select (map (juxt identity identity) [5 10 25 50 100])
          {:selected (:limit @form-state) :on-change #(swap! form-state assoc :limit %)}]]
        [:button.ui.primary.button {:on-click #(do (close-popup)
                                                 (dispatch :split-options-changed dim @form-state))} (t :answer/ok)]
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
