(ns shevek.components.editable-text
  (:require [reagent.core :as r]
            [cuerdas.core :as str]
            [shevek.components.popup :refer [tooltip]]
            [shevek.i18n :refer [t]]))

(defn- input [{:keys [on-save on-stop] :as props}]
  (r/with-let [value (r/atom (:value props))
               stop #(do
                       (reset! value "")
                       (on-stop))
               save #(let [v (-> @value str str/trim)]
                       (when-not (empty? v) (on-save v))
                       (stop))]
    [:div.ui.inverted.transparent.fluid.input
     [:input {:type "text"
              :value @value
              :auto-focus true
              :on-change #(reset! value (.-target.value %))
              :on-blur save
              :on-key-down #(case (.-which %)
                              13 (save)
                              27 (stop)
                              nil)}]]))

(defn editable-text [{:keys [text on-save]}]
  (r/with-let [editing (r/atom false)]
    (if @editing
      [input {:value text
              :on-save on-save
              :on-stop #(reset! editing false)}]
      [:div
       {:on-double-click #(reset! editing true)}
       text
       [:i.pencil.icon
        {:ref (tooltip (t :actions/double-click-edit))}]])))
