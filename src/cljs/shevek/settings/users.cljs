(ns shevek.settings.users
  (:require-macros [reflow.macros :refer [defevh]])
  (:require [shevek.i18n :refer [t]]
            [shevek.components :refer [page-title input-field keyboard-shortcuts focused mail-to]]
            [shevek.lib.react :refer [rmap]]
            [shevek.rpc :as rpc]
            [reagent.core :as r]
            [reflow.db :as db]
            [reflow.core :refer [dispatch]]))

(defevh :users-arrived [db users]
  (-> (assoc db :users users)
      (rpc/loaded :users)))

(defevh :users-requested [db]
  (rpc/call "users.api/find-all" :handler #(dispatch :users-arrived %))
  (rpc/loading db :users))

(defevh :user-saved [db]
  (dispatch :users-requested)
  (rpc/loaded db :saving-user))

(defevh :user-changed [db edited-user cancel]
  (rpc/call "users.api/save" :args [@edited-user]
            :handler #(do (dispatch :user-saved) (cancel)))
  (rpc/loading db :saving-user))

(defevh :user-deleted [db user]
  (rpc/call "users.api/delete" :args [user] :handler #(dispatch :users-requested %))
  db)

(defn- user-form [edited-user]
  (let [cancel #(reset! edited-user nil)
        save #(dispatch :user-changed edited-user cancel)]
    [:div.ui.grid
     [:div.five.wide.column
      [:div.ui.segment.form-container (rpc/loading-class :saving-user)
       [keyboard-shortcuts {:enter save :escape cancel}
        [:div.ui.form
         [focused input-field edited-user :username {:label (t :users/username) :class "required"}]
         [input-field edited-user :fullname {:label (t :users/fullname) :class "required"}]
         [input-field edited-user :password {:label (t :users/password) :class "required"}]
         [input-field edited-user :password-confirmation {:label (t :users/password-confirmation) :class "required"}]
         [input-field edited-user :email {:label (t :users/email)}]
         [:button.ui.primary.button {:on-click save} (t :actions/save)]
         [:button.ui.button {:on-click cancel} (t :actions/cancel)]]]]]]))

(defn- user-row [{:keys [username fullname email] :as original-user} edited-user]
  [:tr
   [:td username]
   [:td fullname]
   [:td (mail-to email)]
   [:td.collapsing
    [:button.ui.compact.basic.button
     {:on-click #(reset! edited-user original-user)}
     (t :actions/edit)]
    [:button.ui.compact.basic.red.button
     {:on-click #(dispatch :user-deleted original-user)}
     (t :actions/delete)]]])

(defn- users-table [edited-user]
  [:table.ui.basic.table
   [:thead>tr
    [:th.three.wide (t :users/username)]
    [:th.five.wide (t :users/fullname)]
    [:th (t :users/email)]
    [:th.right.aligned
     [:button.ui.button {:on-click #(reset! edited-user {})} (t :actions/new)]]]
   [:tbody
    (for [user (db/get :users)]
      ^{:key (:username user)} [user-row user edited-user])]])

(defn users-section []
  (dispatch :users-requested)
  (let [edited-user (r/atom nil)]
    (fn []
      [:section
       [:h2.ui.app.header (t :settings/users)]
       (when @edited-user [user-form edited-user])
       [users-table edited-user]])))
