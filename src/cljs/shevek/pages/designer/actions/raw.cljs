(ns shevek.pages.designer.actions.raw
  (:require [shevek.components.popup :refer [tooltip]]
            [shevek.i18n :refer [t]]
            [shevek.components.modal :refer [show-modal close-modal]]))

(defn- raw-data-dialog [report]
  [:div.ui.modal
   [:div.header (t :raw-data/title)]
   [:div.content (pr-str report)]])

(defn raw-data-button [report]
  [:button.ui.default.icon.button
   {:ref (tooltip (t :raw-data/title))
    :on-click #(show-modal [raw-data-dialog report])}
   [:i.align.justify.icon]])
