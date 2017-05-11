(ns shevek.viewer.filter
  (:require-macros [reflow.macros :refer [defevh]])
  (:require [reagent.core :as r]
            [reflow.core :refer [dispatch]]
            [cuerdas.core :as str]
            [shevek.i18n :refer [t]]
            [shevek.dw :refer [add-dimension remove-dimension replace-dimension time-dimension time-dimension? format-period format-interval to-interval clean-dim]]
            [shevek.lib.react :refer [without-propagation]]
            [shevek.lib.dates :refer [format-date parse-time]]
            [shevek.viewer.shared :refer [panel-header viewer send-main-query send-query format-dimension format-dim-value search-input filter-matching debounce-dispatch highlight current-cube result-value send-pinboard-queries]]
            [shevek.components :refer [controlled-popup select checkbox toggle-checkbox-inside dropdown input-field kb-shortcuts]]))

(defn send-queries [db dim]
  (-> (send-main-query db)
      (send-pinboard-queries dim)))

(defn toggle-filter-value [selected]
  (fnil (if selected conj disj) #{}))

(defevh :dimension-added-to-filter [db {:keys [name] :as dim}]
  (-> (update-in db [:viewer :filter] add-dimension (assoc dim :operator "include"))
      (assoc-in [:viewer :last-added-filter] [name (js/Date.)])))

(defevh :filter-options-changed [db dim opts]
  (-> (update-in db [:viewer :filter] replace-dimension (merge (clean-dim dim) opts))
      (send-queries dim)))

(defevh :dimension-removed-from-filter [db dim]
  (-> (update-in db [:viewer :filter] remove-dimension dim)
      (send-queries dim)))

(defn update-filter-or-remove [dim opts]
  (if (empty? (opts :value))
    (dispatch :dimension-removed-from-filter dim)
    (dispatch :filter-options-changed dim opts)))

(defevh :pinned-dimension-item-toggled [db dim toggled-value selected?]
  (let [already-in-filter? (:value dim)
        toggle (toggle-filter-value selected?)]
    (if already-in-filter?
      (do (update-filter-or-remove dim {:operator (:operator dim) :value (toggle (:value dim) toggled-value)})
        db)
      (-> (update-in db [:viewer :filter] add-dimension (assoc dim :value #{toggled-value}))
          (send-queries dim)))))

(defevh :filter-values-requested [db {:keys [name] :as dim} search]
  (send-query db {:cube (viewer :cube)
                  :filter (cond-> [(first (viewer :filter))]
                                  (seq search) (conj (assoc dim :operator "search" :value search)))
                  :split [(assoc dim :limit 50)]
                  :measures [{:expression "(count)" :name "rowCount"}]}
              [:results :filter name]))

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
    (fn []
      [:div.relative.period-type
       [period-buttons dim showed-period (t :cubes.period/latest)
        [:latest-hour :latest-day :latest-7days :latest-30days :latest-90days]]
       [period-buttons dim showed-period (t :cubes.period/current)
        [:current-day :current-week :current-month :current-quarter :current-year]]
       [period-buttons dim showed-period (t :cubes.period/previous)
        [:previous-day :previous-week :previous-month :previous-quarter :previous-year]]
       [:div.ui.label (if @showed-period
                        (format-period @showed-period (current-cube :max-time))
                        (format-interval interval))]])))

(defn- specific-period-time-filter [{:keys [close]} {:keys [period interval] :as dim}]
  (let [interval (or interval (to-interval period (current-cube :max-time)))
        form-interval (r/atom (zipmap [:from :to] (map format-date interval)))
        parse #(map parse-time ((juxt :from :to) %))
        accept #(dispatch :filter-options-changed dim {:interval (parse @form-interval)})
        shortcuts (kb-shortcuts :enter accept :escape close)]
    (fn []
      (let [[from to] (parse @form-interval)
            valid? (and from to (<= from to))]
        [:div.specific.period-type.ui.form {:ref shortcuts}
         [:div.fields
          [input-field form-interval :from]
          [input-field form-interval :to]]
         [:div
          [:button.ui.primary.compact.button {:on-click accept :class (when-not valid? "disabled")} (t :actions/ok)]
          [:button.ui.compact.button {:on-click (without-propagation close)} (t :actions/cancel)]]]))))

(defn- menu-item-for-period-type [period-type period-type-value]
  [:a.item {:class (when (= @period-type period-type-value) "active")
            :on-click #(reset! period-type period-type-value)}
   (->> (name period-type-value) (str "cubes.period/") keyword t)])

(defn- time-filter-popup [popup {:keys [period] :as dim}]
  (let [period-type (r/atom (if period :relative :specific))]
    (fn []
      [:div.time-filter
       [:div.ui.secondary.pointing.fluid.two.item.menu
        [menu-item-for-period-type period-type :relative]
        [menu-item-for-period-type period-type :specific]]
       (if (= @period-type :relative)
         [relative-period-time-filter dim]
         [specific-period-time-filter popup dim])])))

; TODO PERF cada vez que se tilda un valor se renderizan todos los resultados, ya que todos dependen del filter-opts :value que es donde estan todos los tildados. No se puede evitar?
(defn- dimension-value-item [{:keys [name] :as dim} result filter-opts search]
  (let [value (result-value name result)
        label (format-dimension dim result)]
    [:div.item.has-checkbox {:on-click toggle-checkbox-inside :title label}
     [checkbox (str "cb-filter-" name "-" (str/slug label)) (highlight label search)
      {:checked (some #(= value %) (@filter-opts :value))
       :on-change #(swap! filter-opts update :value (toggle-filter-value %) value)}]]))

(defn filter-operators []
  [[(t :cubes.operator/include) "include"]
   [(t :cubes.operator/exclude) "exclude"]])

(defn- operator-selector [opts]
  [dropdown (filter-operators)
   {:class "icon top left pointing basic compact button"
    :on-change #(swap! opts assoc :operator %)
    :selected (@opts :operator)}
   [:i.icon {:class (case (@opts :operator)
                      "include" "check square"
                      "exclude" "minus square")}]])

(defn- normal-filter-popup [{:keys [close]} {:keys [name] :as dim}]
  (let [opts (r/atom (select-keys dim [:operator :value]))
        search (r/atom "")]
    (fn []
      [:div.ui.form.normal-filter
       [:div.top-inputs
        [operator-selector opts]
        [search-input search {:on-change #(debounce-dispatch :filter-values-requested dim %) :on-stop close}]]
       [:div.items-container
        (into [:div.items]
          (map #(dimension-value-item dim % opts @search)
               (->> (viewer :results :filter name)
                    (filter-matching @search (partial format-dimension dim)))))]
       [:div
        [:button.ui.primary.compact.button
         {:on-click #(update-filter-or-remove dim @opts)
          :class (when (= @opts (select-keys dim [:operator :value])) "disabled")}
         (t :actions/ok)]
        [:button.ui.compact.button
         {:on-click (without-propagation close)}
         (t :actions/cancel)]]])))

(defn- filter-popup [popup dim]
  (if (time-dimension? dim)
    [time-filter-popup popup dim]
    [normal-filter-popup popup dim]))

(defn- filter-title [{:keys [title period interval operator value] :as dim}]
  (let [details (if (= (count value) 1)
                  (-> (first value) (format-dim-value dim) (str/prune 15))
                  (count value))]
    (cond
      period (->> (name period) (str "cubes.period/") keyword t)
      interval (format-interval interval)
      :else [:div title " "
             (when (seq value)
               [:span.details {:class (when (= operator "exclude") "striked")}
                (case operator
                  ("include" "exclude") (str "(" details ")")
                  "")])])))

(defn- filter-item [{:keys [toggle]} dim]
  [:button.ui.green.compact.button.item
   {:class (when-not (time-dimension? dim) "right labeled icon") :on-click toggle}
   (when-not (time-dimension? dim)
     [:i.close.icon {:on-click (without-propagation dispatch :dimension-removed-from-filter dim)}])
   (filter-title dim)])

; TODO Lo del init-open? no me parece muy robusto ni prolijo, pero es lo único que se me ocurre por ahora. Revisar.
(defn filter-panel []
  (let [[last-added-filter last-added-at] (viewer :last-added-filter)
        added-ms-ago (- (js/Date.) last-added-at)]
    [:div.filter.panel
     [panel-header (t :cubes/filter)]
     (for [dim (viewer :filter)]
       ^{:key (:name dim)}
       [(controlled-popup filter-item filter-popup
                          {:position "bottom center"
                           :init-open? (and (= (dim :name) last-added-filter) (< added-ms-ago 250))
                           :on-open #(when-not (time-dimension? dim) (dispatch :filter-values-requested dim ""))
                           :on-close #(when (and (not (time-dimension? dim)) (empty? (dim :value)))
                                        (dispatch :dimension-removed-from-filter dim))})
        dim])]))
