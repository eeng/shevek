(ns shevek.pages.account
  (:require [reagent.core :as r]
            [shevek.reflow.core :refer [dispatch] :refer-macros [defevh]]
            [shevek.i18n :refer [t translation]]
            [shevek.rpc :as rpc]
            [shevek.lib.validation :as v]
            [shevek.lib.auth :refer [current-user]]
            [shevek.components.form :refer [input-field kb-shortcuts]]
            [shevek.components.text :refer [page-title]]
            [shevek.pages.admin.users.form :refer [user-validations]]
            [shevek.lib.notification :refer [notify]]))

(defn account-menu [class]
  [:a.item
   {:href "/account" :class class}
   [:i.user.icon] (:fullname (current-user))])

(defevh :account-saved [db {:keys [error] :as response} edited-user cancel]
  (if error
    (swap! edited-user assoc-in [:errors :current-password] [(translation :account error)])
    (do
      (notify (t :account/saved))
      (dispatch :login-successful response)
      (cancel)))
  (rpc/loaded db :saving-user))

(defevh :account-updated [db edited-user cancel]
  (when (v/valid?! edited-user (assoc user-validations :current-password (v/required)))
    (rpc/call "users/save-account"
              :args [(dissoc @edited-user :password-confirmation)]
              :handler #(dispatch :account-saved % edited-user cancel))
    (rpc/loading db :saving-user)))

(defn account-form [user]
  (let [cancel #(reset! user nil)
        save #(dispatch :account-updated user cancel)
        shortcuts (kb-shortcuts :enter save :escape cancel)]
    (fn []
      [:div.ui.form {:ref shortcuts}
       [input-field user :fullname {:label (t :users/fullname) :class "required" :auto-focus true}]
       [input-field user :email {:label (t :users/email)}]
       [input-field user :current-password {:label (t :account/current-password)
                                            :class "required"
                                            :type "password"}]
       [input-field user :password {:label (t :account/new-password)
                                    :placeholder (t :users/password-hint)
                                    :type "password"}]
       [input-field user :password-confirmation {:label (t :users/password-confirmation)
                                                 :placeholder (t :users/password-hint)
                                                 :type "password"}]
       [:button.ui.primary.button {:on-click save} (t :actions/save)]
       [:button.ui.button {:on-click cancel} (t :actions/cancel)]])))

(defn- detail [i18n-key value]
  (when (seq value)
    [:div.item
     [:div.content
      [:div.meta (t i18n-key)]
      [:div.header value]]]))

(defn account-details [edited-user]
  (let [{:keys [username fullname email]} (current-user)]
    [:div
     [:div.ui.items
      [detail :users/username username]
      [detail :users/fullname fullname]
      [detail :users/email email]
      [detail :users/password "*********"]]
     [:button.ui.primary.button
      {:on-click #(reset! edited-user (current-user))}
      (t :actions/edit)]]))

(defn page []
  (let [user (r/atom nil)]
    (fn []
      [:div.ui.container
       [page-title (t :account/title) (t :account/subtitle) "user"]
       [:div.ui.grid
        [:div.five.wide.column
         [:div.ui.segment (rpc/loading-class :saving-user)
          (if @user
            [account-form user]
            [account-details user])]]]])))
