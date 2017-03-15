(ns pivot.cube
  (:require-macros [reflow.macros :refer [defevh]])
  (:require [reagent.core :as r]
            [pivot.i18n :refer [t]]
            [pivot.rpc :refer [loading?]]
            [pivot.react :refer [rmap]]
            [pivot.rpc :as rpc]
            [pivot.dw :as dw]
            [reflow.db :as db]
            [reflow.core :refer [dispatch]]
            [cuerdas.core :as str]
            [pivot.components :refer [checkbox popup]]))

;; DB model example
#_{:cubes {"wikiticker"
           {:dimensions [{:name "page"}]
            :measures [{:name "added"} {:name "deleted"}]
            :time-boundary {:max-time "..."}}}
   :cube-view {:cube "wikitiker"
               :filter [{:name "__time" :selected-period :latest-day}]
               :split [{:name "region"}]
               :measures [{:name "added"}]
               :pinboard [{:name "channel"}]
               :results {:main {...druid response...}
                         :pinboard {"dim1-name" {...druid response...}
                                    "dim2-name" {...druid response...}}}}}

(defn- cube-view [& keys]
  (db/get-in (into [:cube-view] keys)))

(defn current-cube-name []
  (cube-view :cube))

(defn current-cube [cube-key]
  (-> (db/get :cubes)
      (get (current-cube-name))
      cube-key))

(defn pinboard-measure []
  (first (current-cube :measures)))

(defevh :query-executed2 [db results results-keys]
  (-> (assoc-in db (into [:cube-view] results-keys) results)
      (rpc/loaded results-keys)))

(defn send-query [db q results-keys]
  (rpc/call "dw/query"
            :args [(dw/to-dw-query q)]
            :handler #(dispatch :query-executed2 % results-keys))
  (rpc/loading db results-keys))

(defn- build-time-filter [{:keys [dimensions time-boundary] :as cube}]
  (assoc (dw/time-dimension dimensions)
         :max-time (:max-time time-boundary)
         :selected-period :latest-day))

(defn- init-cube-view [{:keys [cube-view] :as db} {:keys [measures] :as cube}]
  (-> cube-view
      (assoc :filter [(build-time-filter cube)]
             :split []
             :measures (->> measures (take 3) vec))
      (->> (assoc db :cube-view))))

(defn- send-main-query [{:keys [cube-view] :as db}]
  (send-query db cube-view [:results :main]))

(defevh :cube-selected [db cube]
  (rpc/call "dw/cube" :args [cube] :handler #(dispatch :cube-arrived %))
  (dispatch :navigate :cube)
  (-> (assoc db :cube-view {:cube cube})
      (rpc/loading :cube-metadata)))

(defevh :cube-arrived [db {:keys [name] :as cube}]
  (let [cube (dw/set-cube-defaults cube)]
    (-> (assoc-in db [:cubes name] cube)
        (init-cube-view cube)
        (rpc/loaded :cube-metadata)
        (send-main-query))))

(defn includes-dim? [coll dim]
  (some #(dw/dim=? % dim) coll))

(defn add-dimension [coll dim]
  (let [coll (or coll [])]
    (if (includes-dim? coll dim)
      coll
      (conj coll dim))))

(defn remove-dimension [coll dim]
  (vec (remove #(dw/dim=? dim %) coll)))

(defevh :dimension-added-to-filter [db dim]
  (-> (update-in db [:cube-view :filter] add-dimension dim)
      #_(send-main-query)))

(defevh :dimension-removed-from-filter [db dim]
  (-> (update-in db [:cube-view :filter] remove-dimension dim)
      #_(send-main-query)))

(defevh :dimension-added-to-split [db dim]
  (-> (update-in db [:cube-view :split] add-dimension dim)
      #_(send-main-query)))

(defevh :dimension-replaced-split [db dim]
  (-> (assoc-in db [:cube-view :split] [dim])
      #_(send-main-query)))

(defevh :dimension-removed-from-split [db dim]
  (-> (update-in db [:cube-view :split] remove-dimension dim)
      #_(send-main-query)))

(defevh :dimension-pinned [{:keys [cube-view] :as db} {:keys [name] :as dim}]
  (-> (update-in db [:cube-view :pinboard] add-dimension dim)
      (send-query {:cube (:cube cube-view)
                   :filter (:filter cube-view)
                   :split [dim]
                   :measures (vector (pinboard-measure)) ; TODO permitir seleccionar la measure a usar en el pinboard
                   :limit 100}
                  [:results :pinboard name])))

(defevh :measure-toggled [db dim selected]
  (-> (update-in db [:cube-view :measures] (if selected add-dimension remove-dimension) dim)
      (send-main-query)))

(defn- panel-header [text]
  [:h2.ui.sub.header text])

(defn dimension-popup-button [color icon event selected name]
  [:button.ui.circular.icon.button
   {:class color
    :on-click #(do (reset! selected false)
                 (dispatch event name))}
   [:i.icon {:class icon}]])

; TODO Pivot hace una query para traer la cardinality, supongo q x si se actualiza desde que se trajo toda la metadata
(defn- dimension-popup [{:keys [cardinality description] :as dim} selected]
  (let [dim (select-keys dim [:name :title])]
    [:div.ui.special.popup.card {:style {:display (if @selected "block" "none")}}
     (when description
       [:div.content
        [:div.description description]])
     [:div.content
      [dimension-popup-button "green" "filter" :dimension-added-to-filter selected dim]
      [dimension-popup-button "orange" "square" :dimension-replaced-split selected dim]
      [dimension-popup-button "orange" "plus" :dimension-added-to-split selected dim]
      [dimension-popup-button "yellow" "pin" :dimension-pinned selected dim]]
     (when cardinality
       [:div.extra.content (str cardinality " values")])]))

(defn- type-icon [type name]
  (let [effective-type (if (= name "__time") "TIME" type)]
    (condp = effective-type
      "TIME" "wait"
      "LONG" "hashtag"
      "font")))

(defn- dimension-item* [selected {:keys [name title type] :as dimension}]
  [popup
   [:div.item.dimension {:on-click #(swap! selected not)
                         :class (when @selected "active")}
    [:i.icon {:class (type-icon type name)}] title]
   [dimension-popup dimension selected]
   {:on "manual" :position "right center" :distanceAway -30}])

(defn- dimension-item [{:keys [name] :as dimension}]
  (let [selected (r/atom false)
        handle-click-outside (fn [dim-item e]
                               (when-not (.contains (r/dom-node dim-item) (.-target e))
                                 (reset! selected false)))
        node-listener (atom nil)]
    (r/create-class {:reagent-render (partial dimension-item* selected)
                     :component-did-mount #(do
                                             (reset! node-listener (partial handle-click-outside %))
                                             (.addEventListener js/document "click" @node-listener true))
                     :component-will-unmount #(.removeEventListener js/document "click" @node-listener true)})))

(defn- dimensions-panel []
  [:div.dimensions.panel.ui.basic.segment {:class (when (rpc/loading? :cube-metadata) "loading")}
   [panel-header (t :cubes/dimensions)]
   [:div.items
    (rmap dimension-item (current-cube :dimensions))]])

(defn- measure-item [{:keys [title] :as dim}]
  [:div.item {:on-click #(-> % .-target js/$ (.find ".checkbox input") .click)}
   [checkbox title {:checked (includes-dim? (cube-view :measures) dim)
                    :on-change #(dispatch :measure-toggled dim %)}]])

(defn- measures-panel []
  ^{:key (current-cube-name)}
  [:div.measures.panel.ui.basic.segment {:class (when (rpc/loading? :cube-metadata) "loading")}
   [panel-header (t :cubes/measures)]
   [:div.items
    (rmap measure-item (current-cube :measures))]])

(defn- filter-item [{:keys [title] :as dim}]
  [:button.ui.green.compact.button {:class (when-not (dw/time-dimension? dim) "right labeled icon")}
   (when-not (dw/time-dimension? dim)
     [:i.close.icon {:on-click #(dispatch :dimension-removed-from-filter dim)}])
   title])

(defn- filter-panel []
  [:div.filter.panel
   [panel-header (t :cubes/filter)]
   (rmap filter-item (cube-view :filter))])

(defn- split-item [{:keys [title] :as dim}]
  [:button.ui.orange.compact.right.labeled.icon.button
   [:i.close.icon {:on-click #(dispatch :dimension-removed-from-split dim)}]
   title])

(defn- split-panel []
  [:div.split.panel
   [panel-header (t :cubes/split)]
   (rmap split-item (cube-view :split))])

(defn- pinned-dimension-item [dim-name result]
  [:div.item
   [:div.segment-value (-> dim-name keyword result)]
   [:div.measure-value (-> (pinboard-measure) :name keyword result)]])

(defn- pinned-dimension-panel [{:keys [title name] :as dim}]
  [:div.panel.ui.basic.segment {:class (when (rpc/loading? [:results :pinboard name]) "loading")}
   [panel-header title]
   [:div.items
    (rmap (partial pinned-dimension-item name)
          (-> (cube-view :results :pinboard name) first :result))]])

(defn- pinboard-panel []
  [:div.pinboard.zone
   [panel-header (t :cubes/pinboard)]
   (if (seq (cube-view :pinboard))
     (rmap pinned-dimension-panel (cube-view :pinboard))
     [:div.panel.ui.basic.segment.no-pinned
      [:div.icon-hint
       [:i.pin.icon]
       [:div.text (t :cubes/no-pinned)]]])])

(defn- sort-results-according-to-selected-measures [result]
  (let [get-value-for-measure (fn [measure result]
                                (some #(when (= (:name measure) (name (first %)))
                                         (last %))
                                      result))]
    (->> (cube-view :measures)
         (map #(assoc % :value (get-value-for-measure % result))))))

(defn- totals-visualization []
  (let [result (-> (cube-view :results :main) first :result
                   sort-results-according-to-selected-measures)]
    [:div.ui.statistics
     (for [{:keys [name title value]} result]
       ^{:key name}
       [:div.statistic
        [:div.label title]
        [:div.value value]])]))

(defn- visualization-panel []
  [:div.visualization.zone.panel.ui.basic.segment {:class (when (rpc/loading? [:results :main]) "loading")}
   (when (cube-view :results :main)
     (if (empty? (cube-view :measures))
       [:div.icon-hint
        [:i.warning.circle.icon]
        [:div.text (t :cubes/no-measures)]]
       (if (empty? (cube-view :split))
         [totals-visualization]
         [:div "TODO lista"])))])

(defn page []
  [:div#cube-view
   [:div.left-column
    [:div.dimensions-measures.zone
     [dimensions-panel]
     [measures-panel]]]
   [:div.center-column
    [:div.zone
     [filter-panel]
     [split-panel]]
    [visualization-panel]]
   [:div.right-column
    [pinboard-panel]]])
