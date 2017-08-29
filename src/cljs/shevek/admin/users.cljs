(ns shevek.admin.users
  (:require-macros [shevek.reflow.macros :refer [defevh]])
  (:require [reagent.core :as r]
            [cuerdas.core :as str]
            [shevek.i18n :refer [t]]
            [shevek.components.text :refer [page-title mail-to]]
            [shevek.components.form :refer [input-field kb-shortcuts hold-to-confirm search-input filter-matching]]
            [shevek.lib.react :refer [rmap]]
            [shevek.lib.validation :as v]
            [shevek.rpc :as rpc]
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

(defn adapt-for-client [{:keys [allowed-cubes] :or {allowed-cubes "all"} :as user}]
  (let [all-allowed? (= allowed-cubes "all")
        selected-cubes (if all-allowed? [] (map :name allowed-cubes))
        cube-permission (fn [{:keys [name] :as cube}]
                         (assoc cube :selected (includes? selected-cubes name)))]
    (assoc user
           :cubes (mapv cube-permission (cubes-list))
           :only-cubes-selected (not all-allowed?))))

(defn adapt-for-server [{:keys [only-cubes-selected cubes] :as user}]
  (let [allowed-cubes (if only-cubes-selected
                        (->> cubes (filter :selected) (map #(select-keys % [:name])))
                        "all")]
    (-> (dissoc user :password-confirmation :cubes :only-cubes-selected)
        (assoc :allowed-cubes allowed-cubes))))

(def user-validations
  {:username (v/required)
   :fullname (v/required)
   :password (v/regex #"^(?=.*[a-zA-Z])(?=.*[\d!@#\$%\^&\*]).{7,30}$"
                      {:when #(or (new-record? %) (seq (:password %))) :msg :validation/password})
   :password-confirmation (v/confirmation :password {:when (comp seq :password)})
   :email (v/email {:optional? true})})

(defevh :user-changed [db edited-user cancel]
  (when (v/valid?! edited-user user-validations)
    (rpc/call "users.api/save" :args [(adapt-for-server @edited-user)] :handler #(do (dispatch :user-saved) (cancel)))
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
  [:div.item {:on-click #(swap! user update-in [:cubes i :selected] not)
              :class (when-not selected "hidden")}
   [:i.icon.large {:class (if selected "checkmark" "minus")}]
   [:div.content
    [:div.header title]
    [:div.description description]]])

(defn- user-permissions [user]
  (let [{:keys [only-cubes-selected cubes]} @user]
    [:div.permissions-fields
     [:h3.ui.header (t :users/permissions)]
     [:h4.ui.header (t :permissions/allowed-cubes)]
     (if (:admin @user)
       [:div (t :permissions/admin-all-cubes)]
       [:div
        [input-field user :only-cubes-selected
         {:label (t (if only-cubes-selected :permissions/only-cubes-selected :permissions/all-cubes))
          :as :checkbox :input-class "toggle"}]
        (when only-cubes-selected
          [:div.ui.divided.items
           (for [[i {:keys [name] :as cube}] (map-indexed vector cubes)]
             ^{:key name} [cube-permissions user cube i])])])]))

(defn- user-form [user]
  (let [cancel #(reset! user nil)
        save #(dispatch :user-changed user cancel)
        shortcuts (kb-shortcuts :enter save :escape cancel)]
    (fn []
      [:div.ui.padded.segment (rpc/loading-class :saving-user)
       [:div.ui.grid
        [:div.five.wide.column
         [user-fields user shortcuts]]
        [:div.eleven.wide.column.permissions
         [user-permissions user]]
        [:div.sixteen.wide.column
         [:button.ui.primary.button {:on-click save} (t :actions/save)]
         [:button.ui.button {:on-click cancel} (t :actions/cancel)]]]])))

(defn- user-item [{:keys [username fullname email admin allowed-cubes] :or {allowed-cubes "all"} :as original-user} edited-user]
  (let [allowed-cubes-text
        (cond
          (= allowed-cubes "all") (t :permissions/all-cubes)
          (empty? allowed-cubes) (t :permissions/no-cubes)
          :else (str (t :permissions/only-cubes-selected) ": "
                     (str/join ", " (map #(db/get-in [:cubes (:name %) :title]) allowed-cubes))))]
    [:div.item
     [:i.user.huge.icon]
     [:div.content
      [:div.right.floated
       [:button.ui.compact.basic.button
        {:on-click #(reset! edited-user (adapt-for-client original-user))}
        (t :actions/edit)]
       [:button.ui.compact.basic.red.button
        (hold-to-confirm #(dispatch :user-deleted original-user))
        (t :actions/delete)]]
      [:div.header fullname]
      [:div.description
       [:p (t :users/username) ": " username]
       (when (seq email) [:p (t :users/email) ": " (mail-to email)])
       [:p allowed-cubes-text]]
      (when admin
        [:div.extra
         [:div.ui.blue.label "Admin"]])]]))

(defn- users-list [edited-user]
  (let [search (r/atom "")
        username-or-fullname #(str/join "|" ((juxt :username :fullname) %))]
    (fn []
      [:div.users-list
       [search-input search {:wrapper {:class "big"}}]
       [:div.ui.padded.segment
        [:div.ui.divided.items
         (let [users (filter-matching @search username-or-fullname (db/get :users))]
           (if (seq users)
             (for [user users]
               ^{:key (:username user)} [user-item user edited-user])
             [:div.large.tip (t :users/no-results)]))]]])))

(defn users-section []
  (dispatch :users-requested)
  (let [edited-user (r/atom nil)]
    (fn []
      [:section.users
       [:button.ui.button.right.floated
        {:on-click #(reset! edited-user (adapt-for-client default-user))}
        (t :actions/new)]
       [:h2.ui.app.header (t :admin/users)]
       (if @edited-user
         [user-form edited-user]
         [users-list edited-user])])))
