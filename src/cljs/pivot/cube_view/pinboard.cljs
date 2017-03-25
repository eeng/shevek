(ns pivot.cube-view.pinboard
  (:require-macros [reflow.macros :refer [defevh]]
                   [pivot.lib.reagent :refer [rfor]])
  (:require [reagent.core :as r]
            [reflow.core :refer [dispatch]]
            [pivot.i18n :refer [t]]
            [pivot.rpc :refer [loading-class]]
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

(defn- time-granularity-button [dim]
  [dropdown (map (juxt second first) periods)
   {:class "top right pointing" :on-change #(dispatch :pinned-time-granularity-changed dim %)}
   [:i.ellipsis.horizontal.link.icon]])

(defn- search-button [searching]
  [:i.search.link.icon {:on-click #(swap! searching not)}])

(defn- search-input [searching filtered-results]
  (let [search (r/atom nil)]
    (fn []
      (when @searching
        [:div.ui.icon.small.fluid.input.search
         [:input {:type "text" :placeholder (t :input/search)}]
         [:i.search.icon]]))))

(defn- pinned-dimension-panel* [{:keys [title name] :as dim}]
  (let [searching (r/atom false)]
    (fn []
      (let [results (cube-view :results :pinboard name)
            measure (cube-view :pinboard :measure)
            filtered-results (r/atom results)]
        ; TODO quizas convenga poner el loading en los .items, asi se puede cerrar el panel aun si esta cargando.
        [:div.dimension.panel.ui.basic.segment (loading-class [:results :pinboard name])
         [panel-header (str title " " (title-according-to-dim-type dim))
          (if (time-dimension? dim)
            [time-granularity-button dim]
            [search-button searching])
          [:i.close.link.link.icon {:on-click #(dispatch :dimension-unpinned dim)}]]
         [search-input searching filtered-results]
         [:div.items {:class (when (empty? results) "empty")}
          (rfor [result @filtered-results]
            [pinned-dimension-item dim result measure])]]))))

(defn- adjust-max-height [rc]
  (let [panel (-> rc r/dom-node js/$)
        items (-> panel (.find ".header, .item") .toArray js->clj)
        height (reduce + (map #(-> % js/$ .outerHeight) items))]
    (.css panel "max-height", (max (+ height 10) 100))))

(def pinned-dimension-panel
  (with-meta pinned-dimension-panel*
    {:component-did-mount adjust-max-height
     :component-did-update adjust-max-height}))

(defn pinboard-panels []
  [:div.pinboard
   [:div.panel
    [panel-header (t :cubes/pinboard)
     [dropdown (map (juxt :title :name) (current-cube :measures))
      {:selected (cube-view :pinboard :measure :name) :class "top right pointing"
       :on-change #(dispatch :pinboard-measure-selected %)}]]]
   (if (seq (cube-view :pinboard :dimensions))
     (for [dim (cube-view :pinboard :dimensions)]
       ^{:key (dim :name)} [pinned-dimension-panel dim])
     [:div.panel.ui.basic.segment.no-pinned
      [:div.icon-hint
       [:i.pin.icon]
       [:div.text (t :cubes/no-pinned)]]])])
