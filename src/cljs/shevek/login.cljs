(ns shevek.login
  (:require-macros [shevek.reflow.macros :refer [defevh]])
  (:require [reagent.core :as r]
            [shevek.i18n :refer [t]]
            [shevek.reflow.db :as db]
            [shevek.reflow.core :refer [dispatch]]
            [shevek.rpc :as rpc]
            [shevek.components.form :refer [text-input kb-shortcuts]]
            [shevek.lib.session-storage :as session-storage]
            [ajax.core :refer [POST]]
            [cljsjs.jwt-decode]))

(defn current-user []
  (db/get :current-user))

(defn logged-in? []
  (some? (current-user)))

(defn extract-user [token]
  (when token
    (js->clj (js/jwt_decode token) :keywordize-keys true)))

(defonce error (r/atom nil))

(defevh :login-successful [db {:keys [token]}]
  (session-storage/set-item! "shevek.access-token" token)
  (-> (assoc db :current-user (extract-user token))
      (rpc/loaded :logging-in)))

(defevh :login-failed [db]
  (reset! error :users/invalid-credentials)
  (rpc/loaded db :logging-in))

(defevh :login [db user]
  (reset! error nil)
  (POST "/login" {:params @user
                  :handler #(dispatch :login-successful %)
                  :error-handler #(dispatch :login-failed)})
  (rpc/loading db :logging-in))

(defevh :logout [db]
  (session-storage/remove-item! "shevek.access-token")
  (select-keys db [:page :settings]))

(defevh :session-expired [db]
  (reset! error :users/session-expired)
  (dispatch :logout)
  db)

(defevh :user-restored [db]
  (assoc db :current-user (extract-user (session-storage/get-item "shevek.access-token"))))

(defn page []
  (let [user (r/atom {})
        login #(dispatch :login user)
        shortcuts (kb-shortcuts :enter login)]
    (fn []
      [:div#login.ui.center.aligned.grid
       [:div.column
        [:h1.ui.blue.header
         [:i.cubes.icon]
         [:div.content "Shevek"
          [:div.sub.header "Data Warehouse Visualization System"]]]
        [:div.ui.segment (rpc/loading-class :logging-in)
         [:div.ui.form {:ref shortcuts :class (when @error "error")}
          [:div.field
           [:div.ui.left.icon.input
            [:i.user.icon]
            [text-input user :username {:placeholder (t :users/username) :auto-focus true}]]]
          [:div.field
           [:div.ui.left.icon.input
            [:i.lock.icon]
            [text-input user :password {:placeholder (t :users/password) :type "password"}]]]
          (when @error
            [:div.ui.error.message
             (t @error)])
          [:button.ui.fluid.large.blue.primary.button {:on-click login} "Login"]]]]])))
