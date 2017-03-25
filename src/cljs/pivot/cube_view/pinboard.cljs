(ns pivot.cube-view.pinboard
  (:require-macros [reflow.macros :refer [defevh]]
                   [pivot.lib.reagent :refer [rfor]])
  (:require [reagent.core :as r]
            [reflow.core :refer [dispatch]]
            [pivot.i18n :refer [t]]
            [pivot.rpc :refer [loading?]]
            [pivot.lib.react :refer [rmap]]
            [pivot.rpc :as rpc]
            [pivot.dw :refer [find-dimension time-dimension? add-dimension remove-dimension replace-dimension]]
            [pivot.components :refer [dropdown]]
            [pivot.cube-view.shared :refer [current-cube panel-header cube-view send-query format-measure format-dimension]]))

(defn- send-pinned-dim-query [{:keys [cube-view] :as db} {:keys [name] :as dim}]
  (send-query db
              (assoc cube-view
                     :split [(assoc dim :limit 100)]
                     :measures (vector (get-in cube-view [:pinboard :measure])))
              [:results :pinboard name]))

(defn send-pinboard-queries [db]
  (reduce #(send-pinned-dim-query %1 %2) db (cube-view :pinboard :dimensions)))

(defn init-pinned-dim [dim]
  (if (time-dimension? dim)
    (assoc dim :granularity "PT6H" :descending true) ; Default granularity
    dim))

(defevh :dimension-pinned [db dim]
  (let [dim (init-pinned-dim dim)]
    (-> (update-in db [:cube-view :pinboard :dimensions] add-dimension dim)
        (send-pinned-dim-query dim))))

(defevh :dimension-unpinned [db dim]
  (update-in db [:cube-view :pinboard :dimensions] remove-dimension dim))

; TODO al cambiar la measure se muestra temporalmente un cero en todas las filas. Ver si se puede evitar.
(defevh :pinboard-measure-selected [db measure-name]
  (-> (assoc-in db [:cube-view :pinboard :measure]
                (find-dimension measure-name (current-cube :measures)))
      (send-pinboard-queries)))

(defevh :pinned-time-granularity-changed [db dim granularity]
  (let [new-time-dim (assoc dim :granularity granularity)]
    (-> (update-in db [:cube-view :pinboard :dimensions] replace-dimension new-time-dim)
        (send-pinned-dim-query new-time-dim))))

(defn- pinned-dimension-item [dim result measure]
  (let [segment-value (format-dimension dim result)]
    [:div.item {:title segment-value}
     [:div.segment-value segment-value]
     [:div.measure-value (format-measure measure result)]]))

(def periods {"PT1H" "1H"
              "PT6H" "6H"
              "PT12H" "12H"
              "P1D" "1D"
              "P1M" "1M"})

(defn- title-according-to-dim-type [{:keys [granularity] :as dim}]
  (when (time-dimension? dim)
    (str "(" (periods granularity) ")")))

(defn- pinned-dimension-panel [{:keys [title name] :as dim}]
  (let [results (cube-view :results :pinboard name)
        measure (cube-view :pinboard :measure)]
    [:div.panel.ui.basic.segment (rpc/loading-class [:results :pinboard name])
     [panel-header (str title " " (title-according-to-dim-type dim))
      (when (time-dimension? dim)
        [dropdown (map (juxt second first) periods)
         {:class "top right pointing" :on-change #(dispatch :pinned-time-granularity-changed dim %)}
         [:i.ellipsis.horizontal.large.link.icon]])
      [:i.close.link.large.link.icon {:on-click #(dispatch :dimension-unpinned dim)}]]
     [:div.items {:class (when (empty? results) "empty")}
      (rfor [result results]
        [pinned-dimension-item dim result measure])]]))

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
