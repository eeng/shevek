(ns shevek.pages.dashboards.actions.duplicate
  (:require [reagent.core :as r]
            [shevek.rpc :as rpc]
            [shevek.i18n :refer [t]]
            [shevek.components.modal :refer [show-modal close-modal]]
            [shevek.components.form :refer [input-field kb-shortcuts]]
            [shevek.components.notification :refer [notify]]
            [shevek.navigation :refer [navigate]]
            [shevek.pages.dashboards.helpers :refer [slave?]]))

(defn- send-duplicate-request [{:keys [id]} form-data]
  (rpc/call "dashboards/duplicate"
            :args [(assoc form-data :master-id id)]
            :handler (fn [new-dash]
                        (navigate (str "/dashboards/" (:id new-dash)))
                        (notify (t :dashboard/imported))))
  (close-modal))

(defn- duplicate-dialog [{:keys [name] :as dashboard}]
  (r/with-let [form-data (r/atom {:name (str name " Copy")})
               valid? #(seq (:name @form-data))
               save #(when (valid?)
                       (send-duplicate-request dashboard @form-data))
               shortcuts (kb-shortcuts :enter save)]
    [:div.ui.tiny.modal
     [:div.header (t :actions/duplicate)]
     [:div.content
      (when (slave? dashboard)
        [:p (t :dashboard/duplicate-slave-desc)])
      [:div.ui.form {:ref shortcuts}
       [input-field form-data :name {:label (t :dashboards/name)
                                     :auto-focus true
                                     :on-focus #(-> % .-target .select)}]]]
     [:div.actions
      [:button.ui.positive.button
       {:on-click save
        :class (when-not (valid?) "disabled")}
       (t :actions/save)]
      [:button.ui.cancel.button
       (t :actions/cancel)]]]))

(defn duplicate-button [dashboard]
  [:button.ui.default.labeled.icon.button
   {:on-click #(show-modal [duplicate-dialog dashboard])}
   [:i.copy.icon]
   (t :actions/duplicate)])
