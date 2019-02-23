(ns shevek.pages.dashboards.actions.importd
  (:require [reagent.core :as r]
            [shevek.rpc :as rpc]
            [shevek.i18n :refer [t]]
            [shevek.components.modal :refer [show-modal close-modal]]
            [shevek.components.form :refer [input-field]]
            [shevek.components.shortcuts :refer [shortcuts]]
            [shevek.components.notification :refer [notify]]
            [shevek.components.message :refer [info-message]]
            [shevek.navigation :refer [navigate]]))

(defn- send-import-request [{:keys [id]} form-data]
  (rpc/call "dashboards/import"
            :args [(assoc form-data :original-id id)]
            :handler (fn [new-dash]
                        (navigate (str "/dashboards/" (:id new-dash)))
                        (notify (t :dashboard/imported))))
  (close-modal))

(defn- import-dialog [{:keys [name] :as dashboard}]
  (r/with-let [form-data (r/atom {:name name :import-as "link"})
               valid? #(seq (:name @form-data))
               save #(when (valid?)
                       (send-import-request dashboard @form-data))
               active-for #(when (= (:import-as @form-data) %) "active")]
    [:div.ui.tiny.modal
     [:div.header (t :dashboard/import)]

     [:div.content
      [:div.ui.secondary.menu
       [:a.item {:class (active-for "link") :on-click #(swap! form-data assoc :import-as "link")}
        [:i.linkify.icon]
        (t :dashboard/import-as-link)]
       [:a.item {:class (active-for "copy") :on-click #(swap! form-data assoc :import-as "copy")}
        [:i.copy.icon]
        (t :dashboard/import-as-copy)]]
      [:div.ui.tab {:class (active-for "link")}
       [info-message (t :dashboard/import-as-link-desc)]]
      [:div.ui.tab {:class (active-for "copy")}
       [info-message (t :dashboard/import-as-copy-desc)]]
      [:div.ui.hidden.divider]
      [shortcuts {:enter save}
       [:div.ui.form
        [input-field form-data :name {:label (t :dashboard/import-name)
                                      :auto-focus true
                                      :on-focus #(-> % .-target .select)}]]]]
     [:div.actions
      [:button.ui.positive.button
       {:on-click save
        :class (when-not (valid?) "disabled")}
       (t :actions/import)]
      [:button.ui.cancel.button
       (t :actions/cancel)]]]))

(defn import-button [dashboard]
  [:button.ui.green.labeled.icon.button
   {:on-click #(show-modal [import-dialog dashboard])}
   [:i.cloud.download.icon]
   (t :dashboard/import)])
