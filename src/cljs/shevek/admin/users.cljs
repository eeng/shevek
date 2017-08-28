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
            [shevek.lib.util :refer [new-record?]]
            [shevek.lib.string :refer [format-bool]]
            [shevek.lib.collections :refer [includes?]]
            [shevek.lib.dw.cubes :refer [cubes-list]]))

(defevh :users-arrived [db users]
  (-> (assoc db :users users)
      (rpc/loaded :users)))

(defevh :users-requested [db]
  (rpc/call "users.api/find-all" :handler #(dispatch :users-arrived %))
  (rpc/loading db :users))

(defevh :user-saved [db]
  (dispatch :users-requested)
  (rpc/loaded db :saving-user))

(def default-user {:admin false})

(defn permissions->ui [{:keys [allowed-cubes] :or {allowed-cubes "all"}}]
  (let [all-allowed? (= allowed-cubes "all")
        selected-cubes (if all-allowed? [] (map :name allowed-cubes))
        cube-permission (fn [{:keys [name] :as cube}]
                         (assoc cube :selected (includes? selected-cubes name)))]
    {:cubes (mapv cube-permission (cubes-list))
     :only-cubes-selected (not all-allowed?)}))

(defn ui->permissions [{:keys [only-cubes-selected cubes]}]
  (let [allowed-cubes (if only-cubes-selected
                        (->> cubes (filter :selected) (map #(select-keys % [:name])))
                        "all")]
    {:allowed-cubes allowed-cubes}))

(def user-validations
  {:username (v/required)
   :fullname (v/required)
   :password (v/regex #"^(?=.*[a-zA-Z])(?=.*[\d!@#\$%\^&\*]).{7,30}$"
                      {:when #(or (new-record? %) (seq (:password %))) :msg :validation/password})
   :password-confirmation (v/confirmation :password {:when (comp seq :password)})
   :email (v/email {:optional? true})})

(defevh :user-changed [db edited-user cancel]
  (swap! edited-user update :permissions ui->permissions)
  (when (v/valid?! edited-user user-validations)
    (rpc/call "users.api/save"
              :args [(dissoc @edited-user :password-confirmation)]
              :handler #(do (dispatch :user-saved) (cancel)))
    (rpc/loading db :saving-user)))

(defevh :user-deleted [db user]
  (rpc/call "users.api/delete" :args [user] :handler #(dispatch :users-requested %)))

(defn- user-fields [user shortcuts]
  (let [new-user? (new-record? @user)]
    [:div
     [:h3.ui.header (t :users/basic-info)]
     [:div.ui.form {:ref shortcuts}
      [input-field user :username {:label (t :users/username) :class "required" :auto-focus true}]
      [input-field user :fullname {:label (t :users/fullname) :class "required"}]
      [input-field user :password {:label (t :users/password) :class (when new-user? "required")
                                          :type "password" :placeholder (when-not new-user? (t :users/password-hint))}]
      [input-field user :password-confirmation {:label (t :users/password-confirmation)
                                                       :placeholder (when-not new-user? (t :users/password-hint))
                                                       :class (when new-user? "required") :type "password"}]
      [input-field user :email {:label (t :users/email)}]
      [input-field user :admin {:label (t :users/admin) :as :checkbox :input-class "toggle"}]]]))

(defn- cube-permissions [user {:keys [title description selected]} i]
  [:div.item {:on-click #(swap! user update-in [:permissions :cubes i :selected] not)
              :class (when-not selected "hidden")}
   [:i.icon.large {:class (if selected "checkmark" "minus")}]
   [:div.content
    [:div.header title]
    [:div.description description]]])

(defn- user-permissions [user]
  (let [{:keys [only-cubes-selected cubes]} (get-in @user [:permissions])]
    [:div
     [:h3.ui.header (t :users/permissions)]
     [:h4.ui.header (t :permissions/allowed-cubes)]
     [input-field user [:permissions :only-cubes-selected]
      {:label (t (if only-cubes-selected :permissions/only-cubes-selected :permissions/all-cubes))
       :as :checkbox :input-class "toggle"}]
     (when only-cubes-selected
       [:div.ui.divided.items
        (for [[i {:keys [name] :as cube}] (map-indexed vector cubes)]
          ^{:key name} [cube-permissions user cube i])])]))

(defn- user-form [user]
  (let [cancel #(reset! user nil)
        save #(dispatch :user-changed user cancel)
        shortcuts (kb-shortcuts :enter save :escape cancel)]
    (fn []
      [:div.ui.padded.segment (rpc/loading-class :saving-user)
       [:div.ui.grid
        [:div.five.wide.column
         [user-fields user shortcuts]]
        [:div.eleven.wide.column
         [user-permissions user]]
        [:div.sixteen.wide.column
         [:button.ui.primary.button {:on-click save} (t :actions/save)]
         [:button.ui.button {:on-click cancel} (t :actions/cancel)]]]])))

(defn- user-row [{:keys [username fullname email admin] :as original-user} edited-user]
  [:tr
   [:td username]
   [:td fullname]
   [:td (mail-to email)]
   [:td.center.aligned (format-bool admin)]
   [:td.collapsing
    [:button.ui.compact.basic.button
     {:on-click #(reset! edited-user (update original-user :permissions permissions->ui))}
     (t :actions/edit)]
    [:button.ui.compact.basic.red.button
     (hold-to-confirm #(dispatch :user-deleted original-user))
     (t :actions/delete)]]])

(defn- users-table [edited-user]
  [:table.ui.table
   [:thead>tr
    [:th.three.wide (t :users/username)]
    [:th.five.wide (t :users/fullname)]
    [:th (t :users/email)]
    [:th.center.aligned (t :users/admin)]
    [:th.right.aligned
     [:button.ui.button
      {:on-click #(reset! edited-user (update default-user :permissions permissions->ui))}
      (t :actions/new)]]]
   [:tbody
    (for [user (db/get :users)]
      ^{:key (:username user)} [user-row user edited-user])]])

(defn users-section []
  (dispatch :users-requested)
  (let [edited-user (r/atom nil)]
    (fn []
      [:section.users
       [:h2.ui.app.header (t :admin/users)]
       (if @edited-user
         [user-form edited-user]
         [users-table edited-user])])))
