(ns shevek.viewer.filter
  (:require [reagent.core :as r]
            [shevek.reflow.core :refer [dispatch] :refer-macros [defevh defevhi]]
            [cuerdas.core :as str]
            [shevek.i18n :refer [t translation]]
            [shevek.lib.dw.dims :refer [add-dimension remove-dimension replace-dimension time-dimension time-dimension? clean-dim find-dimension merge-dimensions]]
            [shevek.lib.time.ext :refer [format-period format-interval]]
            [shevek.lib.period :refer [to-interval]]
            [shevek.lib.react :refer [without-propagation]]
            [shevek.lib.time :refer [parse-time]]
            [shevek.lib.time.ext :refer [format-date]]
            [shevek.lib.util :refer [debounce trigger-click]]
            [shevek.rpc :as rpc]
            [shevek.viewer.shared :refer [panel-header viewer send-main-query send-query format-dimension format-dim-value highlight current-cube dimension-value send-pinboard-queries filter-title]]
            [shevek.components.form :refer [select checkbox toggle-checkbox-inside dropdown input-field search-input filter-matching classes]]
            [shevek.components.popup :refer [show-popup close-popup]]
            [shevek.components.drag-and-drop :refer [draggable droppable]]
            [shevek.components.calendar :refer [build-range-calendar]]
            [shevek.viewer.url :refer [store-viewer-in-url]]
            [shevek.schemas.conversion :refer [stringify-interval]]
            [com.rpl.specter :refer [transform must]]))

(defn send-queries [db dont-query-pinboard-dim]
  (-> (send-main-query db)
      (send-pinboard-queries dont-query-pinboard-dim)))

(defn toggle-filter-value [selected]
  (fnil (if selected conj disj) #{}))

(def last-added-filter (r/atom nil))

(defn set-as-last-added-filter [name]
  (reset! last-added-filter name))

(defn show-popup-when-added [name node]
  (when (and node (= name @last-added-filter))
     (reset! last-added-filter nil)
     (-> node r/dom-node js/$ trigger-click)))

(defn build-filter [dim opts]
  (merge (clean-dim dim) opts))

(defevhi :dimension-added-to-filter [db dim]
  {:after [store-viewer-in-url]}
  (set-as-last-added-filter (:name dim))
  (update-in db [:viewer :filters] add-dimension (build-filter dim {:operator "include" :value #{}})))

(defevhi :filter-options-changed [db dim opts]
  {:after [store-viewer-in-url]}
  (-> (update-in db [:viewer :filters] replace-dimension (build-filter dim opts))
      (send-queries dim)))

(defevhi :dimension-removed-from-filter [db dim]
  {:after [store-viewer-in-url]}
  (-> (update-in db [:viewer :filters] remove-dimension dim)
      (send-queries dim)))

(defn empty-value? [dim]
  (and (not (time-dimension? dim)) (empty? (:value dim))))

(defn update-filter-or-remove [dim opts]
  (if (empty-value? (merge dim opts))
    (dispatch :dimension-removed-from-filter dim)
    (dispatch :filter-options-changed dim opts)))

(defevhi :pinned-dimension-item-toggled [db dim toggled-value selected?]
  {:after [close-popup store-viewer-in-url]}
  (let [already-in-filter? (:value dim)
        toggle (toggle-filter-value selected?)]
    (if already-in-filter?
      (update-filter-or-remove dim {:operator (:operator dim) :value (toggle (:value dim) toggled-value)})
      (-> (update-in db [:viewer :filters] add-dimension (assoc dim :value #{toggled-value}))
          (send-queries dim)))))

(defn selected-path->filters [selected-path operator]
  (map #(build-filter (first %) {:operator operator :value #{(second %)}}) selected-path))

(defevhi :pivot-table-row-filtered [db selected-path operator]
  {:after [close-popup store-viewer-in-url]}
  (-> (update-in db [:viewer :filters] merge-dimensions (selected-path->filters selected-path operator))
      (send-queries nil)))

(def available-relative-periods
  {"latest-hour" "1H" "latest-day" "1D" "latest-7days" "7D" "latest-30days" "30D" "latest-90days" "90D"
   "current-day" "D" "current-week" "W" "current-month" "M" "current-quarter" "Q" "current-year" "Y"
   "previous-day" "D" "previous-week" "W" "previous-month" "M" "previous-quarter" "Q" "previous-year" "Y"})

(defn- period-buttons [dim showed-period header {:keys [on-filter-change]} periods]
  [:div.periods
   [:h2.ui.sub.header header]
   [:div.ui.five.small.basic.buttons
     (for [period periods
           :let [active? (= period (:period dim))]]
       [:button.ui.button {:key period
                           :class (when active? "active")
                           :on-click #(when-not active? (close-popup) (on-filter-change dim {:period period}))
                           :on-mouse-over #(reset! showed-period period)
                           :on-mouse-out #(reset! showed-period (dim :period))}
        (available-relative-periods period)])]])

(defn- relative-period-time-filter [{:keys [period interval] :as dim} config]
  (let [showed-period (r/atom period)]
    (fn [dim]
      [:div.relative.period-type
       [period-buttons dim showed-period (t :viewer.period/latest) config
        ["latest-hour" "latest-day" "latest-7days" "latest-30days" "latest-90days"]]
       [period-buttons dim showed-period (t :viewer.period/current) config
        ["current-day" "current-week" "current-month" "current-quarter" "current-year"]]
       [period-buttons dim showed-period (t :viewer.period/previous) config
        ["previous-day" "previous-week" "previous-month" "previous-quarter" "previous-year"]]
       [:div.ui.label (if @showed-period
                        (format-period @showed-period (current-cube :max-time))
                        (format-interval interval))]])))

(defn- specific-period-time-filter [{:keys [period interval] :as dim} {:keys [on-filter-change]}]
  (let [interval (or interval (to-interval period (current-cube :max-time)))
        form-interval (r/atom (zipmap [:from :to] (map format-date interval)))
        parse #(map parse-time ((juxt :from :to) %))
        accept (fn []
                 (close-popup)
                 (on-filter-change dim {:interval (parse @form-interval)}))]
    (fn []
      (let [[from to] (parse @form-interval)
            valid? (and from to (<= from to))]
        [:div.specific.period-type.ui.form {:ref build-range-calendar}
         [input-field form-interval :from {:label (t :viewer.period/from) :icon "calendar" :read-only true
                                           :wrapper {:class "left icon calendar from"}}]
         [input-field form-interval :to {:label (t :viewer.period/to) :icon "calendar" :read-only true
                                         :wrapper {:class "left icon calendar to"}}]
         [:div
          [:button.ui.primary.compact.button {:on-click accept :class (when-not valid? "disabled")} (t :actions/ok)]
          [:button.ui.compact.button {:on-click close-popup} (t :actions/cancel)]]]))))

(defn- menu-item-for-period-type [period-type period-type-value]
  [:a.item {:class (when (= @period-type period-type-value) "active")
            :on-click #(reset! period-type period-type-value)}
   (translation :viewer.period (keyword period-type-value))])

(defn- time-filter-popup [{:keys [period] :as dim} config]
  (let [period-type (r/atom (if period :relative :specific))]
    (fn [dim]
      [:div.time-filter
       [:div.ui.secondary.pointing.fluid.two.item.menu
        [menu-item-for-period-type period-type :relative]
        [menu-item-for-period-type period-type :specific]]
       (if (= @period-type :relative)
         [relative-period-time-filter dim config]
         [specific-period-time-filter dim config])])))

(defn- dimension-value-item [{:keys [name] :as dim} result filter search]
  (let [value (dimension-value dim result)
        label (format-dimension dim result)]
    [:div.item.has-checkbox {:on-click toggle-checkbox-inside :title label}
     [checkbox (str "cb-filter-" name "-" (str/slug label)) (highlight label search)
      {:checked (some #(= value %) (@filter :value))
       :on-change #(swap! filter update :value (toggle-filter-value %) value)}]]))

(defn filter-operators []
  [[(t :viewer.operator/include) "include"]
   [(t :viewer.operator/exclude) "exclude"]])

(defn- operator-selector [filter]
  [dropdown (filter-operators)
   {:class "icon top left pointing basic compact button"
    :on-change #(swap! filter assoc :operator %)
    :selected (@filter :operator)}
   [:i.icon {:class (case (@filter :operator)
                      "include" "check square"
                      "exclude" "minus square")}]])

(defn- fetch-dim-values [filter search]
  (let [{:keys [cube name time-filter]} @filter
        time-filter (select-keys (transform (must :interval) stringify-interval time-filter) [:period :interval])
        q {:cube cube
           :filters (cond-> [time-filter]
                            (seq search) (conj {:name name :operator "search" :value search}))
           :splits [{:name name :limit 50}]
           :measures ["rowCount"]}]
    (swap! filter assoc :loading? true)
    (rpc/call "querying/query" :args [q] :handler #(swap! filter assoc :results % :loading? false))))

(defn- normal-filter-popup [dim {:keys [cube time-filter on-filter-change] :as config}]
  (let [filter (-> (select-keys dim [:name :operator :value])
                   (assoc :cube cube :time-filter time-filter)
                   r/atom)
        opts (r/track #(select-keys @filter [:operator :value]))
        search (r/atom "")
        fetch-dim-values-deb (debounce #(fetch-dim-values filter %) 500)]
    (fetch-dim-values filter "")
    (fn [{:keys [name] :as dim}]
      [:div.ui.form.normal-filter
       [:div.top-inputs
        [operator-selector filter]
        [search-input search {:on-change #(fetch-dim-values-deb %)
                              :wrapper {:class (classes "small" (when (@filter :loading?) "loading"))}}]]
       [:div.items-container
        (into [:div.items]
          (map #(dimension-value-item dim % filter @search)
               (filter-matching @search (partial format-dimension dim) (@filter :results))))]
       [:div
        [:button.ui.primary.compact.button
         {:on-click #(do (on-filter-change dim @opts) (close-popup))
          :class (when (= @opts (select-keys dim [:operator :value])) "disabled")}
         (t :actions/ok)]
        [:button.ui.compact.button
         {:on-click close-popup}
         (t :actions/cancel)]]])))

; The time filter use the values in the dim and not an internal r/atom to keep state so we need to use the entire dim as a key so the popup gets rerender when the period change
(defn filter-popup [dim config]
  (if (time-dimension? dim)
    ^{:key (hash dim)} [time-filter-popup dim config]
    ^{:key (:name dim)} [normal-filter-popup dim config]))

(defevh :filter-popup-closed [{:keys [viewer]} {:keys [name]}]
  (if-let [dim (find-dimension name (:filters viewer))]
    (when (empty-value? dim)
      (dispatch :dimension-removed-from-filter dim))))

(defn- filter-item [{:keys [name] :as dim}]
  (let [popup-key (hash {:name name :timestamp (js/Date.)})]
    (fn [dim]
      [:a.ui.green.compact.button.item
       (assoc (draggable dim)
              :class (when-not (time-dimension? dim) "right labeled icon")
              :on-click (fn [el]
                          (show-popup el ^{:key popup-key}
                                      [filter-popup dim {:cube (viewer :cube :name)
                                                         :time-filter (time-dimension (viewer :filters))
                                                         :on-filter-change update-filter-or-remove}]
                                      {:position "bottom center" :on-close #(dispatch :filter-popup-closed dim)}))
              :ref (partial show-popup-when-added name))
       (when-not (time-dimension? dim)
         [:i.close.icon {:on-click (without-propagation dispatch :dimension-removed-from-filter dim)}])
       (filter-title dim)])))

(defn filter-panel []
  [:div.filter.panel (droppable #(dispatch :dimension-added-to-filter %))
   [panel-header (t :viewer/filters)]
   (for [dim (viewer :filters)]
     ^{:key (:name dim)} [filter-item dim])])
