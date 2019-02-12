(ns shevek.pages.dashboards.actions.share
  (:require [shevek.components.popup :refer [tooltip]]
            [shevek.components.modal :refer [show-modal close-modal]]
            [shevek.components.notification :refer [notify]]
            [shevek.components.clipboard :refer [clipboard-button]]
            [shevek.i18n :refer [t]]))

(defn- share-dialog [{:keys [id]}]
  (let [url (str js/location.origin "/dashboards/" id)]
    [:div.ui.tiny.modal
     [:div.header (t :share/title)]
     [:div.content
      [:div.ui.form
       [:div.field
        [:label (t :share/label)]
        [:div.ui.action.input
         [:input#link {:type "text" :read-only true :value url}]
         [clipboard-button
          [:button.ui.green.button
           {:data-clipboard-target "#link"
            :on-click #(do
                         (notify (t :share/copied))
                         (js/setTimeout close-modal 100))}
           (t :share/copy)]]]]]
      [:div.tip.top.spaced (t :dashboard/share-hint)]]]))

(defn share-button [{:keys [id] :as dashboard}]
  [:span
   {:ref (tooltip (t (if id :share/title :dashboard/share-disabled)))}
   [:button.ui.default.icon.button
    {:on-click #(show-modal [share-dialog dashboard] {:autofocus false})
     :data-tid "share"
     :class (when-not id "disabled")}
    [:i.share.alternate.icon]]])
