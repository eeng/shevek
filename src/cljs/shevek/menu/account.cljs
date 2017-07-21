(ns shevek.menu.account
  (:require-macros [shevek.reflow.macros :refer [defevh]])
  (:require [reagent.core :as r]
            [shevek.reflow.core :refer [dispatch]]
            [shevek.i18n :refer [t]]
            [shevek.rpc :as rpc]
            [shevek.lib.validation :as v]
            [shevek.login :refer [current-user]]
            [shevek.components.form :refer [input-field kb-shortcuts]]
            [shevek.components.text :refer [page-title]]
            [shevek.admin.users :refer [validate-user]]
            [shevek.notification :refer [notify]]))

(defn account-menu []
  [:a.item
   {:href "#/account"}
   [:i.user.icon] (:fullname (current-user))])

(defevh :account-saved [db token]
  (notify (t :account/saved))
  (rpc/loaded db :saving-user))

(defevh :account-updated [db edited-user cancel]
  (when (v/valid?! edited-user validate-user)
    (rpc/call "users.api/save-account"
              :args [(dissoc @edited-user :password-confirmation)]
              :handler #(do (dispatch :account-saved) (dispatch :login-successful %) (cancel)))
    (rpc/loading db :saving-user)))

(defn account-form [user]
  (let [cancel #(reset! user nil)
        save #(dispatch :account-updated user cancel)
        shortcuts (kb-shortcuts :enter save :escape cancel)]
    (fn []
      [:div.ui.form {:ref shortcuts}
       [input-field user :username {:label (t :users/username) :class "required" :auto-focus true}]
       [input-field user :fullname {:label (t :users/fullname) :class "required"}]
       [input-field user :password {:label (t :users/password)
                                    :placeholder (t :users/password-hint)
                                    :type "password"}]
       [input-field user :password-confirmation {:label (t :users/password-confirmation)
                                                 :placeholder (t :users/password-hint)
                                                 :type "password"}]
       [input-field user :email {:label (t :users/email)}]
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
         [:div.ui.segment.form-container (rpc/loading-class :saving-user)
          (if @user
            [account-form user]
            [account-details user])]]]])))
