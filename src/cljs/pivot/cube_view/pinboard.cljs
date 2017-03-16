(ns pivot.cube-view.pinboard
  (:require-macros [reflow.macros :refer [defevh]])
  (:require [reagent.core :as r]
            [reflow.core :refer [dispatch]]
            [pivot.i18n :refer [t]]
            [pivot.rpc :refer [loading?]]
            [pivot.lib.react :refer [rmap]]
            [pivot.rpc :as rpc]
            [pivot.dw :refer [find-dimension]]
            [pivot.components :refer [dropdown]]
            [pivot.cube-view.shared :refer [current-cube panel-header cube-view add-dimension remove-dimension send-query format-measure]]))

(defn- send-pinned-dim-query [{:keys [cube-view] :as db} {:keys [name] :as dim}]
  (send-query db
              {:cube (:cube cube-view)
               :filter (:filter cube-view)
               :split [dim]
               :measures (vector (get-in cube-view [:pinboard :measure]))
               :limit 100}
              [:results :pinboard name]))

(defevh :dimension-pinned [db dim]
  (-> (update-in db [:cube-view :pinboard :dimensions] add-dimension dim)
      (send-pinned-dim-query dim)))

(defevh :dimension-unpinned [db dim]
  (update-in db [:cube-view :pinboard :dimensions] remove-dimension dim))

; TODO cuando se pinnea una dim como Page carga rapido igual que pivot pero cdo selecciono una measure aca no. Por que será? UPDATE: tb pasa al pinnear pero si se cambia la pinboard measure antes.
(defevh :pinboard-measure-selected [db measure-name]
  (let [db (assoc-in db [:cube-view :pinboard :measure]
                     (find-dimension measure-name (current-cube :measures)))]
    db
    (reduce #(send-pinned-dim-query %1 %2) db (cube-view :pinboard :dimensions))))

(defn- pinned-dimension-item [dim-name result]
  (let [segment-value (-> dim-name keyword result (or (t :cubes/null-value)))
        measure (cube-view :pinboard :measure)
        measure-value (-> measure :name keyword result (or 0) (format-measure measure))]
    [:div.item {:title segment-value}
     [:div.segment-value segment-value]
     [:div.measure-value measure-value]]))

(defn- pinned-dimension-panel [{:keys [title name] :as dim}]
  (let [results (-> (cube-view :results :pinboard name) first :result)]
    [:div.panel.ui.basic.segment (rpc/loading-class [:results :pinboard name])
     [panel-header title
      [:i.close.link.large.icon {:on-click #(dispatch :dimension-unpinned dim)}]]
     [:div.items {:class (when (empty? results) "empty")}
      (rmap (partial pinned-dimension-item name) results)]]))

; TODO revisar el dropdown q la primera vez q entro a la pag no muestra el selected
(defn pinboard-panel []
  [:div.pinboard.zone
   [panel-header (t :cubes/pinboard)
    [dropdown (map (juxt :title :name) (current-cube :measures))
     {:selected (cube-view :pinboard :measure :name) :class "top right pointing"
      :on-change #(dispatch :pinboard-measure-selected %)}]]
   (if (seq (cube-view :pinboard :dimensions))
     (rmap pinned-dimension-panel (cube-view :pinboard :dimensions))
     [:div.panel.ui.basic.segment.no-pinned
      [:div.icon-hint
       [:i.pin.icon]
       [:div.text (t :cubes/no-pinned)]]])])
