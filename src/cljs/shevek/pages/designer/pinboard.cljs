(ns shevek.pages.designer.pinboard
  (:require [reagent.core :as r]
            [shevek.reflow.core :refer [dispatch] :refer-macros [defevh defevhi]]
            [shevek.rpc :as rpc]
            [shevek.lib.collections :refer [includes?]]
            [shevek.domain.dimension :refer [find-dimension time-dimension? add-dimension remove-dimension replace-dimension clean-dim]]
            [shevek.lib.time.ext :refer [default-granularity]]
            [shevek.i18n :refer [t]]
            [shevek.components.form :refer [dropdown checkbox toggle-checkbox-inside search-input filter-matching]]
            [shevek.components.drag-and-drop :refer [droppable]]
            [shevek.pages.designer.filters :refer [filter-operators]]
            [shevek.pages.designer.helpers :refer [current-cube panel-header format-measure format-dimension search-button highlight debounce-dispatch dimension-value send-pinned-dim-query send-pinboard-queries notify-designer-changes]]
            [shevek.domain.dw :refer [format-measure format-dimension dimension-value]]))

(defn init-pinned-dim [dim designer]
  (let [dim (clean-dim dim)]
    (cond-> (assoc dim :limit 100)
            (time-dimension? dim) (assoc :granularity (default-granularity designer)
                                         :sort-by (assoc dim :descending true)))))

(defevhi :designer/dimension-pinned [{:keys [designer] :as db} dim]
  {:after [notify-designer-changes]}
  (let [dim (init-pinned-dim dim designer)]
    (-> (update-in db [:designer :pinboard :dimensions] add-dimension dim)
        (send-pinned-dim-query dim))))

(defevhi :designer/dimension-unpinned [db dim]
  {:after [notify-designer-changes]}
  (update-in db [:designer :pinboard :dimensions] remove-dimension dim))

(defevh :designer/pinboard-measure-selected [db measure-name]
  (-> (assoc-in db [:designer :pinboard :measure] (find-dimension measure-name (current-cube :measures)))
      (send-pinboard-queries)))

(defevh :designer/pinned-time-granularity-changed [db dim granularity]
  (let [new-time-dim (assoc dim :granularity granularity)]
    (-> (update-in db [:designer :pinboard :dimensions] replace-dimension new-time-dim)
        (send-pinned-dim-query new-time-dim))))

(defevh :designer/dimension-values-searched [db dim search]
  (send-pinned-dim-query db dim (assoc dim :operator "search" :value search)))

(defn- pinned-dimension-item [{:keys [name] :as dim} filter-dim result measure search]
  (let [formatted-value (format-dimension dim result)
        highlighted-value (highlight formatted-value search)
        value (dimension-value dim result)
        toggle-item #(dispatch :designer/pinned-dimension-item-toggled filter-dim value %)
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
    :on-change #(dispatch :designer/pinned-time-granularity-changed dim %)
    :selected granularity}
   [:i.ellipsis.horizontal.link.icon]])

(defn- filter-operators-button [{:keys [operator value] :as filter-dim} unfiltered-operator]
  [dropdown (filter-operators)
   {:class "top right pointing"
    :on-change #(if value
                  (dispatch :designer/filter-options-changed filter-dim {:operator % :value value})
                  (reset! unfiltered-operator %))
    :selected operator}
   [:i.ellipsis.horizontal.link.icon]])

(defn- adjust-max-height [rc]
  (let [panel (-> rc r/dom-node js/$)
        items (-> panel (.find ".header, .items .item, .search.input") .toArray js->clj)
        height (reduce + (map #(-> % js/$ (.outerHeight true)) items))]
    (.css panel "max-height" (max (+ height 10) 100))))

(defn pinned-dimension-panel []
  (let [searching (r/atom false)
        search (r/atom "")
        unfiltered-operator (r/atom "include")]
    (fn [{:keys [title name] :as dim} {:keys [filters] :as designer}]
      (let [measure (get-in designer [:pinboard :measure])
            results (get-in designer [:pinboard-results name])
            filtered-results (filter-matching @search (partial format-dimension dim) results)
            filter-dim (or (find-dimension name filters)
                           (assoc (clean-dim dim) :operator @unfiltered-operator))]
        [:div.dimension.panel.ui.basic.segment
         (merge {:ref #(when % (adjust-max-height %))})
         (when (and (not @searching) (rpc/loading? [:designer :pinboard-results name]))
           [:div.ui.active.loader])
         [panel-header (str title " " (title-according-to-dim-type dim))
          (when-not (time-dimension? dim) [filter-operators-button filter-dim unfiltered-operator])
          (when-not (time-dimension? dim) [search-button searching])
          (when (time-dimension? dim) [time-granularity-button dim])
          [:i.close.link.link.icon {:on-click #(dispatch :designer/dimension-unpinned dim)}]]
         (when @searching
           [search-input search {:on-change #(debounce-dispatch :designer/dimension-values-searched dim %)
                                 :on-stop #(reset! searching false)
                                 :wrapper {:class "small"}}])
         (if results
           (if (seq filtered-results)
             (into [:div.items] (map #(pinned-dimension-item dim filter-dim % measure @search) filtered-results))
             [:div.items [:div.item.no-results (t :errors/no-results)]])
           [:div.items.empty])]))))

(defn pinboard-panel [{:keys [pinboard] :as designer}]
  [:div.pinboard (droppable #(dispatch :designer/dimension-pinned %))
   [:div.panel.header-container
    [panel-header (t :designer/pinboard)
     [dropdown (map (juxt :title :name) (current-cube :measures))
      {:selected (get-in pinboard [:measure :name]) :class "top right pointing"
       :on-change #(dispatch :designer/pinboard-measure-selected %)}]]]
   (if (seq (:dimensions pinboard))
     (for [dim (:dimensions pinboard)]
       ^{:key (dim :name)} [pinned-dimension-panel dim designer])
     [:div.panel.ui.basic.segment.no-pinned
      [:div.icon-hint
       [:i.pin.icon]
       [:div.text (t :designer/no-pinned)]]])])
