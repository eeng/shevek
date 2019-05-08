(ns shevek.pages.designer.filters
  (:require [reagent.core :as r]
            [shevek.reflow.core :refer [dispatch] :refer-macros [defevh defevhi]]
            [cuerdas.core :as str]
            [shevek.i18n :refer [t translation]]
            [shevek.domain.dimension :refer [add-dimension remove-dimension replace-dimension time-dimension time-dimension? clean-dim find-dimension merge-dimensions]]
            [shevek.lib.time.ext :refer [format-period format-interval format-date]]
            [shevek.lib.period :refer [to-interval]]
            [shevek.lib.react :refer [without-propagation]]
            [shevek.lib.time :refer [parse-time]]
            [shevek.lib.util :refer [debounce trigger-click]]
            [shevek.rpc :as rpc]
            [shevek.pages.designer.helpers :refer [panel-header send-designer-query send-query highlight current-cube send-pinboard-queries]]
            [shevek.domain.dw :refer [format-dimension format-dim-value dimension-value]]
            [shevek.components.form :refer [select checkbox toggle-checkbox-inside dropdown input-field search-input filter-matching]]
            [shevek.components.popup :refer [show-popup close-popup tooltip]]
            [shevek.components.drag-and-drop :refer [draggable droppable]]
            [shevek.components.calendar :refer [build-range-calendar]]
            [shevek.schemas.conversion :refer [stringify-interval]]
            [com.rpl.specter :refer [transform must]]))

(defn send-queries [db dont-query-pinboard-dim]
  (-> (send-designer-query db)
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

(defn filter-title [{:keys [title period interval operator value] :as dim}]
  (let [details (if (= (count value) 1)
                  (-> (first value) (format-dim-value dim) (str/prune 15))
                  (count value))]
    (cond
      period [:span (translation :designer.period (keyword period))]
      interval [:span (format-interval interval)]
      :else [:span title " "
             (when (seq value)
               [:span.details (cond-> {}
                                      (= operator "exclude") (assoc :class "striked")
                                      (> (count value) 1) (assoc :ref (tooltip (str/join ", " value))))
                (case operator
                  ("include" "exclude") (str "(" details ")")
                  "")])])))

(defn build-filter [dim opts]
  (merge (clean-dim dim) opts))

(defevh :designer/dimension-added-to-filter [db dim]
  (set-as-last-added-filter (:name dim))
  (update-in db [:designer :filters] add-dimension (build-filter dim {:operator "include" :value #{}})))

(defevh :designer/filter-options-changed [db dim opts]
  (-> (update-in db [:designer :filters] replace-dimension (build-filter dim opts))
      (send-queries dim)))

(defevh :designer/dimension-removed-from-filter [db dim]
  (-> (update-in db [:designer :filters] remove-dimension dim)
      (send-queries dim)))

(defn empty-value? [dim]
  (and (not (time-dimension? dim)) (empty? (:value dim))))

(defn update-filter-or-remove [dim opts]
  (if (empty-value? (merge dim opts))
    (dispatch :designer/dimension-removed-from-filter dim)
    (dispatch :designer/filter-options-changed dim opts)))

(defevhi :designer/pinned-dimension-item-toggled [db dim toggled-value selected?]
  {:after [close-popup]}
  (let [already-in-filter? (:value dim)
        toggle (toggle-filter-value selected?)]
    (if already-in-filter?
      (update-filter-or-remove dim {:operator (:operator dim) :value (toggle (:value dim) toggled-value)})
      (-> (update-in db [:designer :filters] add-dimension (assoc dim :value #{toggled-value}))
          (send-queries dim)))))

(defn slice->filters [slice operator]
  (map #(build-filter (first %) {:operator operator :value #{(second %)}}) slice))

(defevhi :designer/pivot-table-row-filtered [db slice operator]
  {:after [close-popup]}
  (-> (update-in db [:designer :filters] merge-dimensions (slice->filters slice operator))
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

(defn- relative-period-time-filter [{:keys [period interval]} config]
  (let [showed-period (r/atom period)]
    (fn [dim]
      [:div.relative.period-type
       [period-buttons dim showed-period (t :designer.period/latest) config
        ["latest-hour" "latest-day" "latest-7days" "latest-30days" "latest-90days"]]
       [period-buttons dim showed-period (t :designer.period/current) config
        ["current-day" "current-week" "current-month" "current-quarter" "current-year"]]
       [period-buttons dim showed-period (t :designer.period/previous) config
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
                 (on-filter-change dim {:interval (parse @form-interval)}))
        build-calendars (fn [node]
                          (build-range-calendar node {:on-range-changed #(reset! form-interval (zipmap [:from :to] %))}))]
    (fn []
      (let [[from to] (parse @form-interval)
            valid? (and from to (<= from to))]
        [:div.specific.period-type.ui.form {:ref build-calendars}
         [input-field form-interval :from
          {:label (t :designer.period/from) :icon "calendar" :wrapper {:class "left icon calendar from"}}]
         [input-field form-interval :to
          {:label (t :designer.period/to) :icon "calendar" :wrapper {:class "left icon calendar to"}}]
         [:div
          [:button.ui.primary.compact.button {:on-click accept :class (when-not valid? "disabled")} (t :actions/ok)]
          [:button.ui.compact.button {:on-click close-popup} (t :actions/cancel)]]]))))

(defn- menu-item-for-period-type [period-type period-type-value]
  [:a.item {:class (when (= @period-type period-type-value) "active")
            :on-click #(reset! period-type period-type-value)}
   (translation :designer.period (keyword period-type-value))])

(defn- time-filter-popup [{:keys [period]} config]
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
  [[(t :designer.operator/include) "include"]
   [(t :designer.operator/exclude) "exclude"]])

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

(defn- normal-filter-popup [dim {:keys [cube time-filter on-filter-change]}]
  (let [filter (-> (select-keys dim [:name :operator :value])
                   (assoc :cube cube :time-filter time-filter)
                   r/atom)
        opts (r/track #(select-keys @filter [:operator :value]))
        search (r/atom "")
        fetch-dim-values-deb (debounce #(fetch-dim-values filter %) 500)]
    (fetch-dim-values filter "")
    (fn [dim]
      [:div.ui.form.normal-filter
       [:div.top-inputs
        [operator-selector filter]
        [search-input search {:on-change #(fetch-dim-values-deb %)
                              :wrapper {:class ["small" (when (@filter :loading?) "loading")]}}]]
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

(defevh :designer/filter-popup-closed [{:keys [designer]} {:keys [name]}]
  (if-let [dim (find-dimension name (:filters designer))]
    (when (empty-value? dim)
      (dispatch :designer/dimension-removed-from-filter dim))))

(defn- filter-item [{:keys [name]} filters]
  (let [popup-key (hash {:name name :timestamp (js/Date.)})] ; Without the timestamp if a filter was added, changed, removed and then readded, as the key name is the same it would not remount the popup and the previous options would remain.
    (fn [dim]
      [:a.ui.green.compact.button.item
       (assoc (draggable dim)
              :class (when-not (time-dimension? dim) "right labeled icon")
              :on-click (fn [ev]
                          (show-popup ev ^{:key popup-key}
                                      [filter-popup dim {:cube (current-cube :name)
                                                         :time-filter (time-dimension filters)
                                                         :on-filter-change update-filter-or-remove}]
                                      {:position "bottom center"
                                       :on-close #(dispatch :designer/filter-popup-closed dim)
                                       :onVisible #(.focus (js/$ ".normal-filter .search input"))}))
              :ref (partial show-popup-when-added name))
       (when-not (time-dimension? dim)
         [:i.close.icon {:on-click (without-propagation dispatch :designer/dimension-removed-from-filter dim)}])
       (filter-title dim)])))

(defn filters-panel [filters]
  [:div.filter.panel (droppable #(dispatch :designer/dimension-added-to-filter %))
   [panel-header (t :designer/filters)]
   (for [dim filters]
     ^{:key (:name dim)} [filter-item dim filters])])
