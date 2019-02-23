(ns shevek.pages.designer.actions.send-to-dashboard
  (:require [reagent.core :as r]
            [shevek.i18n :refer [t]]
            [shevek.reflow.db :as db]
            [shevek.reflow.core :refer [dispatch]]
            [shevek.components.popup :refer [tooltip]]
            [shevek.components.modal :refer [show-modal close-modal]]
            [shevek.components.form :refer [select kb-shortcuts]]
            [shevek.components.message :refer [info-message]]
            [shevek.components.notification :refer [notify]]
            [shevek.rpc :as rpc]))

(defn- send-to-dashboard [report dashboard-id]
  (close-modal)
  (rpc/call "reports/send-to-dashboard"
            :args [{:report (dissoc report :id) :dashboard-id dashboard-id}]
            :handler #(js/console.log %)))

(defn- dialog [report]
  (r/with-let [_ (dispatch :dashboards/fetch)
               selected-dash (r/atom nil)
               valid? #(some? @selected-dash)
               accept #(send-to-dashboard report @selected-dash)
               shortcuts (kb-shortcuts :enter accept)]
    [:div.ui.tiny.modal
     [:div.header (t :send-to-dashboard/title)]
     [:div.content
      [info-message (t :send-to-dashboard/desc)]
      [:div.ui.form {:ref shortcuts}
       [:div.field
        [:label (t :send-to-dashboard/label)]
        [select (map (juxt :name :id) (db/get :dashboards))
         {:class "search selection"
          :selected @selected-dash
          :on-change #(reset! selected-dash %)}]]]]
     [:div.actions
      [:button.ui.green.button
       {:on-click accept
        :class (when-not (valid?) "disabled")}
       (t :actions/ok)]]]))

(defn send-to-dashboard-button [report]
  [:button.ui.default.icon.button
   {:on-click #(show-modal [dialog report])
    :ref (tooltip (t :send-to-dashboard/title))
    :data-tid "send-to-dashboard"}
   [:i.share.square.icon]])
