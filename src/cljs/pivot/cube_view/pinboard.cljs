(ns pivot.cube-view.pinboard
  (:require-macros [reflow.macros :refer [defevh]])
  (:require [reagent.core :as r]
            [reflow.core :refer [dispatch]]
            [pivot.i18n :refer [t]]
            [pivot.rpc :refer [loading?]]
            [pivot.lib.react :refer [rmap]]
            [pivot.rpc :as rpc]
            [pivot.components :refer [dropdown]]
            [pivot.cube-view.shared :refer [current-cube panel-header cube-view add-dimension remove-dimension send-query]]))

(defn pinboard-measure []
  (first (current-cube :measures)))

(defn- pinned-dimension-query [dim cube-view]
  {:cube (:cube cube-view)
   :filter (:filter cube-view)
   :split [dim]
   :measures (vector (pinboard-measure)) ; TODO permitir seleccionar la measure a usar en el pinboard
   :limit 100})

(defevh :dimension-pinned [{:keys [cube-view] :as db} {:keys [name] :as dim}]
  (-> (update-in db [:cube-view :pinboard] add-dimension dim)
      (send-query (pinned-dimension-query dim cube-view)
                  [:results :pinboard name])))

(defn- pinned-dimension-item [dim-name result]
  (let [segment-value (-> dim-name keyword result)]
    [:div.item {:title segment-value}
     [:div.segment-value segment-value]
     [:div.measure-value (-> (pinboard-measure) :name keyword result)]]))

(defn- pinned-dimension-panel [{:keys [title name] :as dim}]
  (let [results (-> (cube-view :results :pinboard name) first :result)]
    [:div.panel.ui.basic.segment (rpc/loading-class [:results :pinboard name])
     [panel-header title
      [:i.close.link.large.icon {:on-click #(dispatch :dimension-unpinned dim)}]]
     [:div.items {:class (when (empty? results) "empty")}
      (rmap (partial pinned-dimension-item name) results)]]))

(defn pinboard-panel []
  [:div.pinboard.zone
   [panel-header (t :cubes/pinboard)
    [dropdown (map (juxt :title :name) (cube-view :measures))
     {:selected "count" :class "top right pointing"}]]
   (if (seq (cube-view :pinboard))
     (rmap pinned-dimension-panel (cube-view :pinboard))
     [:div.panel.ui.basic.segment.no-pinned
      [:div.icon-hint
       [:i.pin.icon]
       [:div.text (t :cubes/no-pinned)]]])])
