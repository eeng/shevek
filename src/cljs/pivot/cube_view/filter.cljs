(ns pivot.cube-view.filter
  (:require-macros [pivot.lib.reagent :refer [rfor]]
                   [reflow.macros :refer [defevh]])
  (:require [reagent.core :as r]
            [reflow.core :refer [dispatch]]
            [cuerdas.core :as str]
            [pivot.i18n :refer [t]]
            [pivot.dw :refer [add-dimension remove-dimension replace-dimension time-dimension? format-period]]
            [pivot.lib.react :refer [rmap without-propagation]]
            [pivot.cube-view.shared :refer [panel-header cube-view send-main-query send-query format-dimension]]
            [pivot.cube-view.pinboard :refer [send-pinboard-queries]]
            [pivot.components :refer [controlled-popup select checkbox toggle-checkbox-inside dropdown]]))

(defn init-filtered-dim [dim]
  (assoc dim :operator "include"))

(defevh :dimension-added-to-filter [db dim]
  (update-in db [:cube-view :filter] add-dimension (init-filtered-dim dim)))

(defevh :dimension-removed-from-filter [db dim]
  (-> (update-in db [:cube-view :filter] remove-dimension dim)
      (send-main-query)
      (send-pinboard-queries)))

(defevh :filter-options-changed [db dim opts]
  (-> (update-in db [:cube-view :filter] replace-dimension (merge dim opts))
      (send-main-query)
      (send-pinboard-queries)))

(defevh :filter-values-requested [db {:keys [name] :as dim}]
  (send-query db {:cube (cube-view :cube)
                  :filter [(first (cube-view :filter))]
                  :split [(assoc dim :limit 100)]
                  :measures [{:type "count" :name "rowCount"}]}
              [:results :filter name]))

(def available-relative-periods
  {:latest-hour "1H" :latest-6hours "6H" :latest-day "1D" :latest-7days "7D" :latest-30days "30D"
   :current-day "D" :current-week "W" :current-month "M" :current-quarter "Q" :current-year "Y"
   :previous-day "D" :previous-week "W" :previous-month "M" :previous-quarter "Q" :previous-year "Y"})

(defn- period-buttons [{:keys [selected-period] :as dim} showed-period header periods]
  [:div.periods
   [:h2.ui.sub.header header]
   [:div.ui.five.small.basic.buttons
     (rfor [period periods]
       [:button.ui.button {:class (when (= period selected-period) "active")
                           :on-click #(when-not (= period selected-period)
                                        (dispatch :filter-options-changed dim {:selected-period period}))
                           :on-mouse-over #(reset! showed-period period)
                           :on-mouse-out #(reset! showed-period selected-period)}
        (available-relative-periods period)])]])

(defn- relative-period-time-filter [{:keys [selected-period max-time] :as dim}]
  (let [showed-period (r/atom selected-period)]
    (fn []
      [:div.relative.period-type
       [period-buttons dim showed-period (t :cubes.period/latest)
        [:latest-hour :latest-6hours :latest-day :latest-7days :latest-30days]]
       [period-buttons dim showed-period (t :cubes.period/current)
        [:current-day :current-week :current-month :current-quarter :current-year]]
       [period-buttons dim showed-period (t :cubes.period/previous)
        [:previous-day :previous-week :previous-month :previous-quarter :previous-year]]
       [:div.ui.label (format-period @showed-period max-time)]])))

(defn- specific-period-time-filter []
  [:div.specific.period-type "TODO"])

(defn- menu-item-for-period-type [period-type period-type-value]
  [:a.item {:class (when (= @period-type period-type-value) "active")
            :on-click #(reset! period-type period-type-value)}
   (->> (name period-type-value) (str "cubes.period/") keyword t)])

(defn- time-filter-popup [dim]
  (let [period-type (r/atom :relative)] ; TODO aca habria que tomar el valor de la dim pero solo al abrirse el popup... mm
    (fn []
      [:div.time-filter
       [:div.ui.secondary.pointing.fluid.two.item.menu
        [menu-item-for-period-type period-type :relative]
        [menu-item-for-period-type period-type :specific]]
       (if (= @period-type :relative)
         [relative-period-time-filter dim]
         [specific-period-time-filter dim])])))

; TODO PERF cada vez que se tilda un valor se renderizan todos los resultados, ya que todos dependen del filter-opts :value que es donde estan todos los tildados. No se puede evitar?
(defn- dimension-value-item [{:keys [name] :as dim} result filter-opts]
  (let [value (-> name keyword result)
        label (format-dimension dim result)]
    [:div.item {:on-click toggle-checkbox-inside}
     [checkbox label
      {:checked (some #(= value %) (@filter-opts :value))
       :on-change #(swap! filter-opts update :value (fnil (if % conj disj) #{}) value)
       :name (str name "-" (str/slug label))}]]))

(defn- operator-selector [opts]
  [dropdown [[(t :cubes.operator/include) "include"]
             [(t :cubes.operator/exclude) "exclude"]]
   {:class "icon left pointing basic compact button"
    :on-change #(swap! opts assoc :operator %)}
   [:i.icon {:class (case (@opts :operator)
                      "include" "check square"
                      "exclude" "minus square")}]])

(defn- search-input []
  [:div.ui.icon.input
   [:input {:type "text" :placeholder "Search"}]
   [:i.search.icon]])

(defn- normal-filter-popup [close-popup {:keys [name] :as dim}]
  (let [opts (r/atom (select-keys dim [:operator :value]))]
    (fn []
      [:div.ui.form.normal-filter
       [:div.top-inputs
        [operator-selector opts]
        [search-input]]
       [:div.items-container
         [:div.items
          (rfor [result (cube-view :results :filter name)]
            [dimension-value-item dim result opts])]]
       [:div
        [:button.ui.primary.compact.button
         {:on-click #(do (close-popup) (dispatch :filter-options-changed dim @opts))}
         (t :answer/ok)]
        [:button.ui.compact.button {:on-click (without-propagation close-popup)} (t :answer/cancel)]]])))

(defn- filter-popup [{:keys [close opened?]} dim]
  [:div.ui.special.popup {:style {:display (if opened? "block" "none")}}
   (if (time-dimension? dim)
     [time-filter-popup dim]
     [normal-filter-popup close dim])])

(defn- filter-title [{:keys [title selected-period operator value] :as dim}]
  (if (time-dimension? dim)
    (->> (name selected-period) (str "cubes.period/") keyword t)
    [:div title " "
     (when (seq value)
       [:span.details {:class (when (= operator "exclude") "striked")}
        (case operator
          ("include" "exclude") (str "(" (count value) ")")
          "")])]))

(defn- filter-item [{:keys [toggle]} dim]
  [:button.ui.green.compact.button.item
   {:class (when-not (time-dimension? dim) "right labeled icon") :on-click toggle}
   (when-not (time-dimension? dim)
     [:i.close.icon {:on-click (without-propagation dispatch :dimension-removed-from-filter dim)}])
   (filter-title dim)])

(defn filter-panel []
  [:div.filter.panel
   [panel-header (t :cubes/filter)]
   (rfor [dim (cube-view :filter)]
     [controlled-popup filter-item filter-popup
      {:position "bottom center"
       :on-open #(when-not (time-dimension? dim) (dispatch :filter-values-requested dim))}
      dim])])
