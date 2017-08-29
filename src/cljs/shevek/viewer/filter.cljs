(ns shevek.viewer.filter
  (:require-macros [shevek.reflow.macros :refer [defevh defevhi]])
  (:require [reagent.core :as r]
            [shevek.reflow.core :refer [dispatch]]
            [cuerdas.core :as str]
            [shevek.i18n :refer [t translation]]
            [shevek.lib.dw.dims :refer [add-dimension remove-dimension replace-dimension time-dimension time-dimension? clean-dim find-dimension merge-dimensions]]
            [shevek.lib.dw.time :refer [format-period format-interval to-interval]]
            [shevek.lib.react :refer [without-propagation]]
            [shevek.lib.dates :refer [format-date parse-date]]
            [shevek.viewer.shared :refer [panel-header viewer send-main-query send-query format-dimension format-dim-value debounce-dispatch highlight current-cube dimension-value send-pinboard-queries filter-title]]
            [shevek.components.form :refer [select checkbox toggle-checkbox-inside dropdown input-field search-input filter-matching]]
            [shevek.components.popup :refer [show-popup close-popup]]
            [shevek.components.drag-and-drop :refer [draggable droppable]]
            [shevek.components.calendar :refer [build-range-calendar]]
            [shevek.viewer.url :refer [store-viewer-in-url]]))

(defn send-queries [db dont-query-pinboard-dim]
  (-> (send-main-query db)
      (send-pinboard-queries dont-query-pinboard-dim)))

(defn toggle-filter-value [selected]
  (fnil (if selected conj disj) #{}))

(def last-added-filter (r/atom nil))

(defn build-filter [dim opts]
  (merge (clean-dim dim) opts))

(defevhi :dimension-added-to-filter [db {:keys [name] :as dim}]
  {:after [store-viewer-in-url]}
  (reset! last-added-filter name)
  (update-in db [:viewer :filter] add-dimension (build-filter dim {:operator "include" :value #{}})))

(defevhi :filter-options-changed [db dim opts]
  {:after [close-popup store-viewer-in-url]}
  (-> (update-in db [:viewer :filter] replace-dimension (build-filter dim opts))
      (send-queries dim)))

(defevhi :dimension-removed-from-filter [db dim]
  {:after [store-viewer-in-url]}
  (-> (update-in db [:viewer :filter] remove-dimension dim)
      (send-queries dim)))

(defn update-filter-or-remove [dim opts]
  (if (empty? (opts :value))
    (dispatch :dimension-removed-from-filter dim)
    (dispatch :filter-options-changed dim opts)))

(defevhi :pinned-dimension-item-toggled [db dim toggled-value selected?]
  {:after [close-popup store-viewer-in-url]}
  (let [already-in-filter? (:value dim)
        toggle (toggle-filter-value selected?)]
    (if already-in-filter?
      (update-filter-or-remove dim {:operator (:operator dim) :value (toggle (:value dim) toggled-value)})
      (-> (update-in db [:viewer :filter] add-dimension (assoc dim :value #{toggled-value}))
          (send-queries dim)))))

(defevhi :pivot-table-row-filtered [db filter-path operator]
  {:after [close-popup store-viewer-in-url]}
  (let [new-filters (map #(build-filter (first %) {:operator operator :value #{(second %)}}) filter-path)]
    (-> (update-in db [:viewer :filter] merge-dimensions new-filters)
        (send-queries nil))))

(defevh :filter-values-requested [db {:keys [name] :as dim} search]
  (send-query db {:cube (viewer :cube)
                  :filter (cond-> [(first (viewer :filter))]
                                  (seq search) (conj (assoc dim :operator "search" :value search)))
                  :split [(assoc dim :limit 50)]
                  :measures [{:expression "(count)" :name "rowCount"}]}
              [:filter name]))

(def available-relative-periods
  {:latest-hour "1H" :latest-day "1D" :latest-7days "7D" :latest-30days "30D" :latest-90days "90D"
   :current-day "D" :current-week "W" :current-month "M" :current-quarter "Q" :current-year "Y"
   :previous-day "D" :previous-week "W" :previous-month "M" :previous-quarter "Q" :previous-year "Y"})

(defn- period-buttons [dim showed-period header periods]
  [:div.periods
   [:h2.ui.sub.header header]
   [:div.ui.five.small.basic.buttons
     (for [period periods]
       [:button.ui.button {:key period
                           :class (when (= period (:period dim)) "active")
                           :on-click #(when-not (= period (:period dim))
                                        (dispatch :filter-options-changed dim {:period period}))
                           :on-mouse-over #(reset! showed-period period)
                           :on-mouse-out #(reset! showed-period (dim :period))}
        (available-relative-periods period)])]])

(defn- relative-period-time-filter [{:keys [period interval] :as dim}]
  (let [showed-period (r/atom period)]
    (fn [dim]
      [:div.relative.period-type
       [period-buttons dim showed-period (t :viewer.period/latest)
        [:latest-hour :latest-day :latest-7days :latest-30days :latest-90days]]
       [period-buttons dim showed-period (t :viewer.period/current)
        [:current-day :current-week :current-month :current-quarter :current-year]]
       [period-buttons dim showed-period (t :viewer.period/previous)
        [:previous-day :previous-week :previous-month :previous-quarter :previous-year]]
       [:div.ui.label (if @showed-period
                        (format-period @showed-period (current-cube :max-time))
                        (format-interval interval))]])))

(defn- specific-period-time-filter [{:keys [period interval] :as dim}]
  (let [interval (or interval (to-interval period (current-cube :max-time)))
        form-interval (r/atom (zipmap [:from :to] (map format-date interval)))
        parse #(map parse-date ((juxt :from :to) %))
        accept #(dispatch :filter-options-changed dim {:interval (parse @form-interval)})]
    (fn []
      (let [[from to] (parse @form-interval)
            valid? (and from to (<= from to))]
        [:div.specific.period-type.ui.form {:ref build-range-calendar}
         [input-field form-interval :from {:label (t :viewer.period/from) :icon "calendar" :read-only true
                                           :input-wrapper {:class "left icon calendar from"}}]
         [input-field form-interval :to {:label (t :viewer.period/to) :icon "calendar" :read-only true
                                         :input-wrapper {:class "left icon calendar to"}}]
         [:div
          [:button.ui.primary.compact.button {:on-click accept :class (when-not valid? "disabled")} (t :actions/ok)]
          [:button.ui.compact.button {:on-click close-popup} (t :actions/cancel)]]]))))

(defn- menu-item-for-period-type [period-type period-type-value]
  [:a.item {:class (when (= @period-type period-type-value) "active")
            :on-click #(reset! period-type period-type-value)}
   (translation :viewer.period period-type-value)])

(defn- time-filter-popup [{:keys [period] :as dim}]
  (let [period-type (r/atom (if period :relative :specific))]
    (fn [dim]
      [:div.time-filter
       [:div.ui.secondary.pointing.fluid.two.item.menu
        [menu-item-for-period-type period-type :relative]
        [menu-item-for-period-type period-type :specific]]
       (if (= @period-type :relative)
         [relative-period-time-filter dim]
         [specific-period-time-filter dim])])))

; TODO PERF cada vez que se tilda un valor se renderizan todos los resultados, ya que todos dependen del filter-opts :value que es donde estan todos los tildados. No se puede evitar?
(defn- dimension-value-item [{:keys [name] :as dim} result filter-opts search]
  (let [value (dimension-value dim result)
        label (format-dimension dim result)]
    [:div.item.has-checkbox {:on-click toggle-checkbox-inside :title label}
     [checkbox (str "cb-filter-" name "-" (str/slug label)) (highlight label search)
      {:checked (some #(= value %) (@filter-opts :value))
       :on-change #(swap! filter-opts update :value (toggle-filter-value %) value)}]]))

(defn filter-operators []
  [[(t :viewer.operator/include) "include"]
   [(t :viewer.operator/exclude) "exclude"]])

(defn- operator-selector [opts]
  [dropdown (filter-operators)
   {:class "icon top left pointing basic compact button"
    :on-change #(swap! opts assoc :operator %)
    :selected (@opts :operator)}
   [:i.icon {:class (case (@opts :operator)
                      "include" "check square"
                      "exclude" "minus square")}]])

(defn- normal-filter-popup [dim]
  (let [opts (r/atom (select-keys dim [:operator :value]))
        search (r/atom "")]
    (dispatch :filter-values-requested dim "")
    (fn [{:keys [name] :as dim}]
      [:div.ui.form.normal-filter
       [:div.top-inputs
        [operator-selector opts]
        [search-input search {:on-change #(debounce-dispatch :filter-values-requested dim %) :wrapper {:class "small"}}]]
       [:div.items-container
        (into [:div.items]
          (map #(dimension-value-item dim % opts @search)
               (->> (viewer :results :filter name)
                    (filter-matching @search (partial format-dimension dim)))))]
       [:div
        [:button.ui.primary.compact.button
         {:on-click #(do (update-filter-or-remove dim @opts) (close-popup))
          :class (when (= @opts (select-keys dim [:operator :value])) "disabled")}
         (t :actions/ok)]
        [:button.ui.compact.button
         {:on-click close-popup}
         (t :actions/cancel)]]])))

; The time filter use the values in the dim and not an internal r/atom to keep state so we need to use the entire dim as a key so the popup gets rerender when the period change
(defn- filter-popup [dim]
  (if (time-dimension? dim)
    ^{:key (hash dim)} [time-filter-popup dim]
    ^{:key (:name dim)} [normal-filter-popup dim]))

(defevh :filter-popup-closed [{:keys [viewer]} {:keys [name]}]
  (if-let [dim (find-dimension name (:filter viewer))]
    (when (and (not (time-dimension? dim)) (empty? (dim :value)))
      (dispatch :dimension-removed-from-filter dim))))

(defn- filter-item [{:keys [name]}]
  (let [popup-key (hash {:name name :timestamp (js/Date.)})
        show-popup-when-added #(when (and % (= name @last-added-filter))
                                 (reset! last-added-filter nil)
                                 (-> % r/dom-node js/$ (.find "span") .click))]
    (fn [dim]
      [:a.ui.green.compact.button.item
       (assoc (draggable dim)
              :class (when-not (time-dimension? dim) "right labeled icon")
              :on-click (fn [el] (show-popup el ^{:key popup-key} [filter-popup dim]
                                             {:position "bottom center" :on-close #(dispatch :filter-popup-closed dim)}))
              :ref show-popup-when-added)
       (when-not (time-dimension? dim)
         [:i.close.icon {:on-click (without-propagation dispatch :dimension-removed-from-filter dim)}])
       (filter-title dim)])))

(defn filter-panel []
  [:div.filter.panel (droppable #(dispatch :dimension-added-to-filter %))
   [panel-header (t :viewer/filter)]
   (for [dim (viewer :filter)]
     ^{:key (:name dim)} [filter-item dim])])
