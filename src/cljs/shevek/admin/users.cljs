(ns shevek.admin.users
  (:require-macros [shevek.reflow.macros :refer [defevh]])
  (:require [shevek.i18n :refer [t]]
            [shevek.components.text :refer [page-title mail-to]]
            [shevek.components.form :refer [input-field kb-shortcuts hold-to-confirm]]
            [shevek.lib.react :refer [rmap]]
            [shevek.lib.validation :as v]
            [shevek.rpc :as rpc]
            [reagent.core :as r]
            [shevek.reflow.db :as db]
            [shevek.reflow.core :refer [dispatch]]
            [shevek.lib.util :refer [new-record?]]))

(defevh :users-arrived [db users]
  (-> (assoc db :users users)
      (rpc/loaded :users)))

(defevh :users-requested [db]
  (rpc/call "users.api/find-all" :handler #(dispatch :users-arrived %))
  (rpc/loading db :users))

(defevh :user-saved [db]
  (dispatch :users-requested)
  (rpc/loaded db :saving-user))

(def user-validations
  {:username (v/required)
   :fullname (v/required)
   :password (v/regex #"^(?=.*[a-zA-Z])(?=.*[\d!@#\$%\^&\*]).{7,30}$"
                      {:when #(or (new-record? %) (seq (:password %))) :msg :validation/password})
   :password-confirmation (v/confirmation :password {:when (comp seq :password)})
   :email (v/email {:optional? true})})

(defevh :user-changed [db edited-user cancel]
  (when (v/valid?! edited-user user-validations)
    (rpc/call "users.api/save"
              :args [(dissoc @edited-user :password-confirmation)]
              :handler #(do (dispatch :user-saved) (cancel)))
    (rpc/loading db :saving-user)))

(defevh :user-deleted [db user]
  (rpc/call "users.api/delete" :args [user] :handler #(dispatch :users-requested %)))

(defn- user-form [edited-user]
  (let [cancel #(reset! edited-user nil)
        save #(dispatch :user-changed edited-user cancel)
        shortcuts (kb-shortcuts :enter save :escape cancel)]
    (fn []
      (let [new-user? (new-record? @edited-user)]
        [:div.ui.grid
         [:div.five.wide.column
          [:div.ui.segment (rpc/loading-class :saving-user)
           [:div.ui.form {:ref shortcuts}
            [input-field edited-user :username {:label (t :users/username) :class "required" :auto-focus true}]
            [input-field edited-user :fullname {:label (t :users/fullname) :class "required"}]
            [input-field edited-user :password {:label (t :users/password) :class (when new-user? "required")
                                                :type "password" :placeholder (when-not new-user? (t :users/password-hint))}]
            [input-field edited-user :password-confirmation {:label (t :users/password-confirmation)
                                                             :placeholder (when-not new-user? (t :users/password-hint))
                                                             :class (when new-user? "required") :type "password"}]
            [input-field edited-user :email {:label (t :users/email)}]
            [:button.ui.primary.button {:on-click save} (t :actions/save)]
            [:button.ui.button {:on-click cancel} (t :actions/cancel)]]]]]))))

(defn- user-row [{:keys [username fullname email] :as original-user} edited-user]
  (let [holding (r/atom nil)]
    (fn []
      [:tr
       [:td username]
       [:td fullname]
       [:td (mail-to email)]
       [:td.collapsing
        [:button.ui.compact.basic.button
         {:on-click #(reset! edited-user original-user)}
         (t :actions/edit)]
        [:button.ui.compact.basic.red.button
         (hold-to-confirm holding #(dispatch :user-deleted original-user))
         (t :actions/delete)]]])))

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
       [:h2.ui.app.header (t :admin/users)]
       (when @edited-user [user-form edited-user])
       [users-table edited-user]])))
