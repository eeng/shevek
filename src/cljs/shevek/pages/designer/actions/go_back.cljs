(ns shevek.pages.designer.actions.go-back
  (:require [shevek.i18n :refer [t]]
            [shevek.components.popup :refer [tooltip]]))

(defn go-back-button []
  [:button.ui.icon.green.button
   {:on-click #(.back js/history)
    :ref (tooltip (t :designer/go-back))
    :data-tid "go-back"}
   [:i.reply.icon]])
