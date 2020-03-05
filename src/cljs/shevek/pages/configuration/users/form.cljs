(ns shevek.pages.configuration.users.form
  (:require [shevek.i18n :refer [t]]
            [shevek.components.form :refer [input-field]]
            [shevek.components.shortcuts :refer [shortcuts]]
            [shevek.lib.validation :as v]
            [shevek.lib.collections :refer [assoc-nil find-by]]
            [shevek.rpc :as rpc]
            [shevek.reflow.core :refer [dispatch] :refer-macros [defevh]]
            [shevek.lib.util :refer [new-record?]]
            [shevek.pages.cubes.helpers :refer [cubes-list]]
            [shevek.pages.configuration.users.permissions :refer [user-permissions]]
            [shevek.schemas.conversion :refer [unparse-filters report-dims->designer]]
            [shevek.schemas.user :refer [CubePermissions]]
            [schema-tools.core :as st]))

(defevh :user-saved [db]
  (dispatch :users/fetch)
  (rpc/loaded db :saving-user))

(defn adapt-for-client [{:keys [allowed-cubes] :or {allowed-cubes "all"} :as user}]
  (let [cube-permission (fn [{:keys [name] :as cube}]
                          (let [allowed-cube (find-by :name name allowed-cubes)
                                allowed-measures (get allowed-cube :measures "all")]
                            (-> cube
                                (assoc :selected (some? allowed-cube)
                                       :only-measures-selected (not= "all" allowed-measures)
                                       :allowed-measures (when (not= "all" allowed-measures) allowed-measures)
                                       :filters (report-dims->designer (:filters allowed-cube) cube)))))]
    (assoc user
           :cubes (mapv cube-permission (cubes-list))
           :only-cubes-selected (not= allowed-cubes "all"))))

(defn adapt-for-server [{:keys [only-cubes-selected cubes] :as user}]
  (let [adapt-cube (fn [{:keys [name only-measures-selected allowed-measures filters]}]
                     (-> {:name name}
                         (assoc :measures (if (and only-measures-selected (seq allowed-measures))
                                            allowed-measures
                                            "all")
                                :filters (unparse-filters filters))
                         (st/select-schema CubePermissions)))
        allowed-cubes (if only-cubes-selected
                        (->> cubes (filter :selected) (map adapt-cube))
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
    (rpc/call "users/save" :args [(adapt-for-server @edited-user)] :handler #(do (dispatch :user-saved) (cancel)))
    (rpc/loading db :saving-user)))

(defn- user-fields [user]
  (let [new-user? (new-record? @user)]
    [:div
     [:h3.ui.orange.header (t :users/basic-info)]
     [:div.ui.form
      [input-field user :username
       {:label (t :users/username) :class "required" :auto-focus true}]
      [input-field user :fullname
       {:label (t :users/fullname) :class "required"}]
      [input-field user :password
       {:label (t :users/password) :class (when new-user? "required")
        :type "password" :placeholder (when-not new-user? (t :users/password-hint))}]
      [input-field user :password-confirmation
       {:label (t :users/password-confirmation)
        :placeholder (when-not new-user? (t :users/password-hint))
        :class (when new-user? "required") :type "password"}]
      [input-field user :email
       {:label (t :users/email)}]
      [input-field user :admin
       {:label (t :users/admin) :as :checkbox :input {:class "toggle"} :data-tid "user-admin-cb"}]]]))

(defn user-form [user]
  (let [cancel #(reset! user nil)
        save #(dispatch :user-changed user cancel)]
    (fn []
      [shortcuts {:enter save :escape cancel}
       [:div.ui.segment.user-form (rpc/loading-class :saving-user)
        [:div.ui.relaxed.grid
         [:div.five.wide.column
          [user-fields user]]
         [:div.eleven.wide.column.permissions
          [user-permissions user]]
         [:div.sixteen.wide.column
          [:button.ui.primary.button {:on-click save} (t :actions/save)]
          [:button.ui.button {:on-click cancel} (t :actions/cancel)]]]]])))
