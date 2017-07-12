(ns shevek.viewer.pinboard
  (:require-macros [reflow.macros :refer [defevh]])
  (:require [reagent.core :as r]
            [reflow.core :refer [dispatch]]
            [shevek.lib.util :refer [debounce regex-escape]]
            [shevek.lib.collections :refer [includes?]]
            [shevek.lib.react :refer [without-propagation]]
            [shevek.i18n :refer [t]]
            [shevek.rpc :refer [loading-class]]
            [shevek.lib.dw.dims :refer [find-dimension time-dimension? add-dimension remove-dimension replace-dimension clean-dim]]
            [shevek.lib.dw.time :refer [default-granularity]]
            [shevek.components :refer [dropdown checkbox toggle-checkbox-inside]]
            [shevek.components.drag-and-drop :refer [droppable]]
            [shevek.viewer.filter :refer [filter-operators]]
            [shevek.viewer.shared :refer [current-cube panel-header viewer send-query format-measure format-dimension filter-matching search-button search-input highlight debounce-dispatch result-value send-pinned-dim-query send-pinboard-queries]]))

(defn init-pinned-dim [dim viewer]
  (cond-> (assoc (clean-dim dim) :limit 100)
          (time-dimension? dim) (assoc :granularity (default-granularity viewer)
                                       :sort-by (assoc dim :descending true))))

(defevh :dimension-pinned [{:keys [viewer] :as db} dim]
  (let [dim (init-pinned-dim dim viewer)]
    (-> (update-in db [:viewer :pinboard :dimensions] add-dimension dim)
        (send-pinned-dim-query dim))))

(defevh :dimension-unpinned [db dim]
  (update-in db [:viewer :pinboard :dimensions] remove-dimension dim))

; TODO al cambiar la measure se muestra temporalmente un cero en todas las filas. Ver si se puede evitar.
(defevh :pinboard-measure-selected [db measure-name]
  (-> (assoc-in db [:viewer :pinboard :measure]
                (find-dimension measure-name (current-cube :measures)))
      (send-pinboard-queries)))

(defevh :pinned-time-granularity-changed [db dim granularity]
  (let [new-time-dim (assoc dim :granularity granularity)]
    (-> (update-in db [:viewer :pinboard :dimensions] replace-dimension new-time-dim)
        (send-pinned-dim-query new-time-dim))))

(defevh :dimension-values-searched [db dim search]
  (send-pinned-dim-query db dim (assoc dim :operator "search" :value search)))

(defn- pinned-dimension-item [{:keys [name] :as dim} filter-dim result measure search]
  (let [formatted-value (format-dimension dim result)
        highlighted-value (highlight formatted-value search)
        value (result-value name result)
        toggle-item #(dispatch :pinned-dimension-item-toggled filter-dim value %)
        show-checkbox? (seq (:value filter-dim))]
    [:div.item {:title formatted-value :on-click (cond
                                                   (time-dimension? dim) identity
                                                   show-checkbox? toggle-checkbox-inside
                                                   :else #(toggle-item true))}
     (if show-checkbox?
      [checkbox (str "cb-pinboard-item-" name "-" value) highlighted-value
       {:checked (includes? (:value filter-dim) value) :on-change toggle-item}]
      highlighted-value)
     [:div.measure-value (format-measure measure result)]]))

(def periods {"PT1H" "1H"
              "PT6H" "6H"
              "PT12H" "12H"
              "P1D" "1D"
              "P1M" "1M"})

(defn- title-according-to-dim-type [{:keys [granularity] :as dim}]
  (when (time-dimension? dim)
    (str "(" (periods granularity) ")")))

(defn- time-granularity-button [{:keys [granularity] :as dim}]
  [dropdown (map (juxt second first) periods)
   {:class "top right pointing"
    :on-change #(dispatch :pinned-time-granularity-changed dim %)
    :selected granularity}
   [:i.ellipsis.horizontal.link.icon]])

(defn- filter-operators-button [{:keys [operator value] :as filter-dim} unfiltered-operator]
  [dropdown (filter-operators)
   {:class "top right pointing"
    :on-change #(if value
                  (dispatch :filter-options-changed filter-dim {:operator % :value value})
                  (reset! unfiltered-operator %))
    :selected operator}
   [:i.ellipsis.horizontal.link.icon]])

(defn- adjust-max-height [rc]
  (let [panel (-> rc r/dom-node js/$)
        items (-> panel (.find ".header, .items .item, .search.input") .toArray js->clj)
        height (reduce + (map #(-> % js/$ (.outerHeight true)) items))]
    (.css panel "max-height", (max (+ height 10) 100))))

(defn pinned-dimension-panel [_]
  (let [searching (r/atom false)
        search (r/atom "")
        unfiltered-operator (r/atom "include")]
    (fn [{:keys [title name] :as dim}]
      (let [measure (viewer :pinboard :measure)
            results (viewer :results :pinboard name)
            filtered-results (filter-matching @search (partial format-dimension dim) results)
            filter-dim (or (find-dimension name (viewer :filter))
                           (assoc (clean-dim dim) :operator @unfiltered-operator))]
        [:div.dimension.panel.ui.basic.segment
         (merge {:ref #(when % (adjust-max-height %))}
                (when-not @searching (loading-class [:results :pinboard name])))
         [panel-header (str title " " (title-according-to-dim-type dim))
          (when-not (time-dimension? dim) [filter-operators-button filter-dim unfiltered-operator])
          (when-not (time-dimension? dim) [search-button searching])
          (when (time-dimension? dim) [time-granularity-button dim])
          [:i.close.link.link.icon {:on-click #(dispatch :dimension-unpinned dim)}]]
         (when @searching
           [search-input search {:on-change #(debounce-dispatch :dimension-values-searched dim %)
                                 :on-stop #(reset! searching false)}])
         (if results
           (if (seq filtered-results)
             (into [:div.items] (map #(pinned-dimension-item dim filter-dim % measure @search) filtered-results))
             [:div.items [:div.item.no-results (t :cubes/no-results)]])
           [:div.items.empty])]))))

(defn pinboard-panels []
  [:div.pinboard (droppable #(dispatch :dimension-pinned %))
   [:div.panel
    [panel-header (t :cubes/pinboard)
     [dropdown (map (juxt :title :name) (current-cube :measures))
      {:selected (viewer :pinboard :measure :name) :class "top right pointing"
       :on-change #(dispatch :pinboard-measure-selected %)}]]]
   (if (seq (viewer :pinboard :dimensions))
     (for [dim (viewer :pinboard :dimensions)]
       ^{:key (dim :name)} [pinned-dimension-panel dim])
     [:div.panel.ui.basic.segment.no-pinned
      [:div.icon-hint
       [:i.pin.icon]
       [:div.text (t :cubes/no-pinned)]]])])
