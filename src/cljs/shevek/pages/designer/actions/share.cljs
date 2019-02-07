(ns shevek.pages.designer.actions.share
  (:require [shevek.components.popup :refer [tooltip]]
            [shevek.i18n :refer [t]]
            [shevek.components.modal :refer [show-modal close-modal]]))

(defn- share-dialog [report]
  [:div.ui.tiny.modal
   [:div.header (t :share/title)]
   [:div.content (pr-str report)]])

(defn share-button [report]
  [:button.ui.default.icon.button
   {:ref (tooltip (t :share/title))
    :on-click #(show-modal [share-dialog report])}
   [:i.share.alternate.icon]])
