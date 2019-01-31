(ns shevek.pages.profile.password
  (:require [reagent.core :as r]
            [shevek.i18n :refer [t translation]]
            [shevek.domain.auth :refer [current-user]]
            [shevek.reflow.core :refer [dispatch] :refer-macros [defevh]]
            [shevek.components.form :refer [input-field kb-shortcuts]]
            [shevek.rpc :as rpc]
            [shevek.lib.validation :as v]
            [shevek.pages.configuration.users.form :refer [user-validations]]
            [shevek.components.notification :refer [notify]]))

(defn- handle-response [{:keys [error] :as response} user]
  (if error
    (swap! user assoc-in [:errors :current-password] [(translation :account error)])
    (do
      (notify (t :account/saved))
      (dispatch :login-successful response)))
  (swap! user assoc :loading? false :current-password nil))

(defevh :account/save [db user cancel]
  (when (v/valid?! user (assoc user-validations :current-password (v/required)))
    (rpc/call "users/save-account"
              :args [(dissoc @user :password-confirmation :loading?)]
              :handler #(handle-response % user))
    (swap! user assoc :loading? true)
    db))

(defn password-panel []
  (r/with-let [user (r/atom (current-user))
               save #(dispatch :account/save user)
               shortcuts (kb-shortcuts :enter save)]
    [:div.ui.form.change-password {:ref shortcuts}
     [input-field user :current-password {:label (t :account/current-password)
                                          :class "required"
                                          :type "password"
                                          :auto-focus true}]
     [input-field user :password {:label (t :account/new-password)
                                  :placeholder (t :users/password-hint)
                                  :type "password"}]
     [input-field user :password-confirmation {:label (t :users/password-confirmation)
                                               :placeholder (t :users/password-hint)
                                               :type "password"}]
     [input-field user :fullname {:label (t :users/fullname) :class "required"}]
     [input-field user :email {:label (t :users/email)}]
     [:button.ui.primary.button
      {:on-click save :class (when (:loading? @user) "loading")}
      (t :actions/save)]]))
