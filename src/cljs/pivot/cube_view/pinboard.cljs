(ns pivot.cube-view.pinboard
  (:require-macros [reflow.macros :refer [defevh]])
  (:require [reagent.core :as r]
            [reflow.core :refer [dispatch]]
            [pivot.i18n :refer [t]]
            [pivot.rpc :refer [loading?]]
            [pivot.lib.react :refer [rmap]]
            [pivot.rpc :as rpc]
            [pivot.dw :refer [find-dimension time-dimension? dim=? add-dimension remove-dimension]]
            [pivot.lib.collections :refer [replace-when]]
            [pivot.components :refer [dropdown]]
            [pivot.cube-view.shared :refer [current-cube panel-header cube-view send-query format-measure format-dimension]]))

(defn- send-pinned-dim-query [{:keys [cube-view] :as db} {:keys [name] :as dim}]
  (send-query db
              (assoc cube-view
                     :split [(assoc dim :limit 50)]
                     :measures (vector (get-in cube-view [:pinboard :measure])))
              [:results :pinboard name]))

(defn init-pinned-dimension [dim]
  (if (time-dimension? dim)
    (assoc dim :granularity "PT6H" :descending true) ; Default granularity
    dim))

(defevh :dimension-pinned [db dim]
  (let [dim (init-pinned-dimension dim)]
    (-> (update-in db [:cube-view :pinboard :dimensions] add-dimension dim)
        (send-pinned-dim-query dim))))

(defevh :dimension-unpinned [db dim]
  (update-in db [:cube-view :pinboard :dimensions] remove-dimension dim))

(defevh :pinboard-measure-selected [db measure-name]
  (let [db (assoc-in db [:cube-view :pinboard :measure]
                     (find-dimension measure-name (current-cube :measures)))]
    db
    (reduce #(send-pinned-dim-query %1 %2) db (cube-view :pinboard :dimensions))))

(defevh :pinned-time-granularity-changed [db dim granularity]
  (let [new-time-dim (assoc dim :granularity granularity)]
    (-> (update-in db [:cube-view :pinboard :dimensions]
                   #(replace-when (partial dim=? new-time-dim) (constantly new-time-dim) %))
        (send-pinned-dim-query new-time-dim))))

(defn- pinned-dimension-item [dim result]
  (let [segment-value (-> (dim :name) keyword result (format-dimension dim))
        measure (cube-view :pinboard :measure)
        measure-value (-> measure :name keyword result (format-measure measure))]
    [:div.item {:title segment-value}
     [:div.segment-value segment-value]
     [:div.measure-value measure-value]]))

(def periods {"PT1H" "1H"
              "PT6H" "6H"
              "PT12H" "12H"
              "P1D" "1D"
              "P1M" "1M"})

(defn- title-according-to-dim-type [{:keys [granularity] :as dim}]
  (when (time-dimension? dim)
    (str "(" (periods granularity) ")")))

(defn- pinned-dimension-panel [{:keys [title name] :as dim}]
  (let [results (cube-view :results :pinboard name)]
    [:div.panel.ui.basic.segment (rpc/loading-class [:results :pinboard name])
     [panel-header (str title " " (title-according-to-dim-type dim))
      (when (time-dimension? dim)
        [dropdown (map (juxt second first) periods)
         {:class "top right pointing" :on-change #(dispatch :pinned-time-granularity-changed dim %)}
         [:i.ellipsis.horizontal.large.link.icon]])
      [:i.close.link.large.link.icon {:on-click #(dispatch :dimension-unpinned dim)}]]
     [:div.items {:class (when (empty? results) "empty")}
      (rmap (partial pinned-dimension-item dim) results)]]))

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
