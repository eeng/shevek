(ns pivot.cube-view.filter-split
  (:require-macros [pivot.lib.reagent :refer [rfor]]
                   [reflow.macros :refer [defevh]])
  (:require [reagent.core :as r]
            [reflow.core :refer [dispatch]]
            [pivot.i18n :refer [t]]
            [pivot.dw :as dw]
            [pivot.lib.react :refer [rmap]]
            [pivot.lib.collections :refer [replace-when]]
            [pivot.rpc :as rpc]
            [pivot.cube-view.shared :refer [panel-header cube-view send-main-query]]
            [pivot.components :refer [with-controlled-popup]]))

(defevh :time-period-changed [db period]
  (-> (update-in db [:cube-view :filter]
                 (fn [dims] (replace-when dw/time-dimension? #(assoc % :selected-period period) dims)))
      (send-main-query)))

(def available-relative-periods
  {:latest-hour "1H" :latest-6hours "6H" :latest-day "1D" :latest-7days "7D" :latest-30days "30D"
   :current-day "D" :current-week "W" :current-month "M" :current-quarter "Q" :current-year "Y"
   :previous-day "D" :previous-week "W" :previous-month "M" :previous-quarter "Q" :previous-year "Y"})

(defn- period-buttons [header {:keys [selected-period]} periods]
  [:div.periods
   [:h2.ui.sub.header header]
   [:div.ui.five.small.basic.buttons
     (rfor [period periods]
       [:button.ui.button {:class (when (= period selected-period) "active")
                           :on-click #(when-not (= period selected-period)
                                        (dispatch :time-period-changed period))}
        (available-relative-periods period)])]])

(defn- relative-period-time-filter [dim]
  [:div.relative.period-type
   [period-buttons (t :cubes.period/latest) dim
    [:latest-hour :latest-6hours :latest-day :latest-7days :latest-30days]]
   [period-buttons (t :cubes.period/current) dim
    [:current-day :current-week :current-month :current-quarter :current-year]]
   [period-buttons (t :cubes.period/previous) dim
    [:previous-day :previous-week :previous-month :previous-quarter :previous-year]]])

(defn- specific-period-time-filter []
  [:div.specific.period-type "Specific..."])

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

(defn- other-filter-popup [dim]
  [:div (pr-str dim)])

(defn- filter-popup [selected dim]
  [:div.ui.special.popup {:style {:display (if @selected "block" "none")}}
   (if (dw/time-dimension? dim)
     [time-filter-popup dim]
     [other-filter-popup dim])])

(defn- filter-title [{:keys [title] :as dim}]
  (if (dw/time-dimension? dim)
    title
    title))

(defn- filter-item [selected dim]
  [:button.ui.green.compact.button.item
   {:class (when-not (dw/time-dimension? dim) "right labeled icon")
    :on-click #(swap! selected not)}
   (when-not (dw/time-dimension? dim)
     [:i.close.icon {:on-click #(dispatch :dimension-removed-from-filter dim)}])
   (filter-title dim)])

(defn filter-panel []
  [:div.filter.panel
   [panel-header (t :cubes/filter)]
   (rmap (with-controlled-popup filter-item filter-popup {:position "bottom center"}) (cube-view :filter))])

(defn- split-item [{:keys [title] :as dim}]
  [:button.ui.orange.compact.right.labeled.icon.button
   [:i.close.icon {:on-click #(dispatch :dimension-removed-from-split dim)}]
   title])

(defn split-panel []
  [:div.split.panel
   [panel-header (t :cubes/split)]
   (rmap split-item (cube-view :split))])
