(ns shevek.admin.users.form
  (:require [reagent.core :as r]
            [shevek.i18n :refer [t]]
            [shevek.components.form :refer [input-field kb-shortcuts]]
            [shevek.lib.validation :as v]
            [shevek.lib.collections :refer [assoc-nil]]
            [shevek.rpc :as rpc]
            [shevek.reflow.core :refer [dispatch] :refer-macros [defevh]]
            [shevek.lib.util :refer [new-record?]]
            [shevek.lib.collections :refer [find-by]]
            [shevek.lib.dw.cubes :refer [cubes-list]]
            [shevek.admin.users.permissions :refer [user-permissions]]))

(defevh :user-saved [db]
  (dispatch :users-requested)
  (rpc/loaded db :saving-user))

(defn adapt-for-client [{:keys [allowed-cubes] :or {allowed-cubes "all"} :as user}]
  (let [cube-permission (fn [{:keys [name] :as cube}]
                          (let [allowed-cube (find-by :name name allowed-cubes)
                                allowed-measures (get allowed-cube :measures "all")]
                            (-> cube
                                (assoc :selected (some? allowed-cube)
                                       :only-measures-selected (not= "all" allowed-measures)
                                       :allowed-measures (when (not= "all" allowed-measures) allowed-measures))
                                (assoc-nil :filters []))))]
    (assoc user
           :cubes (mapv cube-permission (cubes-list))
           :only-cubes-selected (not= allowed-cubes "all"))))

(defn adapt-for-server [{:keys [only-cubes-selected cubes] :as user}]
  (let [adapt-cube (fn [{:keys [name allowed-measures only-measures-selected]}]
                     {:name name :measures (if only-measures-selected allowed-measures "all")})
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
      [input-field user :admin {:label (t :users/admin) :as :checkbox :input {:class "toggle"}}]]]))

(defn user-form [user]
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
