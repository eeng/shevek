(ns shevek.components.confirmation
  (:require [reagent.core :as r]
            [shevek.components.popup :refer [show-popup close-popup]]
            [shevek.i18n :refer [t]]))

(defn with-confirm [[comp props children]]
  (r/with-let [original-handler (:on-click props)
               confirm-button [:button.ui.red.tiny.compact.button
                               {:on-click #(do (original-handler) (close-popup))}
                               (t :actions/confirm)]]
    [comp
     (assoc props :on-click #(show-popup % confirm-button {:position "bottom center" :class "confirm"}))
     children]))
