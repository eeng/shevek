(ns shevek.pages.designer.actions.send-to-dashboard
  (:require [reagent.core :as r]
            [shevek.i18n :refer [t]]
            [shevek.reflow.db :as db]
            [shevek.reflow.core :refer [dispatch]]
            [shevek.components.popup :refer [tooltip]]
            [shevek.components.modal :refer [show-modal close-modal]]
            [shevek.components.form :refer [select input-field]]
            [shevek.components.message :refer [info-message]]
            [shevek.components.shortcuts :refer [shortcuts]]
            [shevek.components.notification :refer [notify]]
            [shevek.rpc :as rpc]))

(defn- send-to-dashboard [form-data]
  (rpc/call "dashboards/receive-report"
            :args [form-data]
            :handler #(do
                        (close-modal)
                        (notify (t :send-to-dashboard/success)))))

(defn- dialog [report]
  (r/with-let [_ (dispatch :dashboards/fetch)
               form-data (r/atom {:report report :dashboard-id nil})
               valid? #(some? (:dashboard-id @form-data))
               accept #(when (valid?) (send-to-dashboard @form-data))]
    [shortcuts {:enter accept}
     [:div.ui.tiny.modal
      [:div.header (t :send-to-dashboard/title)]
      [:div.content
       [info-message (t :send-to-dashboard/desc)]
       [:div.ui.form
        [:div.field
         [:label (t :send-to-dashboard/label)]
         [select (map (juxt :name :id) (db/get :dashboards))
          {:class "search selection"
           :selected (:dashboard-id @form-data)
           :on-change #(swap! form-data assoc :dashboard-id %)}]]
        [input-field form-data [:report :name] {:label (t :dashboard/import-name)}]]]
      [:div.actions
       [:button.ui.green.button
        {:on-click accept
         :class (when-not (valid?) "disabled")}
        (t :actions/ok)]]]]))

(defn send-to-dashboard-button [report]
  [:button.ui.default.icon.button
   {:on-click #(show-modal [dialog report])
    :ref (tooltip (t :send-to-dashboard/title))
    :data-tid "send-to-dashboard"}
   [:i.share.square.icon]])
