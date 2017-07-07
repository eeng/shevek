(ns shevek.login
  (:require-macros [reflow.macros :refer [defevh]])
  (:require [reagent.core :as r]
            [shevek.i18n :refer [t]]
            [reflow.db :as db]
            [reflow.core :refer [dispatch]]
            [shevek.rpc :as rpc]
            [shevek.components :refer [text-input focused kb-shortcuts]]
            [shevek.lib.local-storage :as local-storage]
            [ajax.core :refer [POST]]
            [clojure.string :as str]
            [goog.crypt.base64 :as b64]))

(defn current-user []
  (db/get :current-user))

(defn logged-in? []
  (some? (current-user)))

(defn extract-user [token]
  (when token
    (as-> token $
          (str/split $ #"\.")
          (second $)
          (b64/decodeString $)
          (.parse js/JSON $)
          (js->clj $ :keywordize-keys true))))

(defevh :login-successful [db {:keys [token]}]
  (local-storage/set-item! "shevek.access-token" token)
  (-> (assoc db :current-user (extract-user token))
      (rpc/loaded :logging-in)))

(defevh :login-failed [db user]
  (swap! user assoc :error :users/invalid-credentials)
  (rpc/loaded db :logging-in))

(defevh :login [db user]
  (swap! user dissoc :error)
  (POST "/login" {:params (dissoc @user :error)
                  :handler #(dispatch :login-successful %)
                  :error-handler #(dispatch :login-failed user)})
  (rpc/loading db :logging-in))

(defevh :logout [db]
  (local-storage/remove-item! "shevek.access-token")
  (dissoc db :current-user))

(defevh :user-restored [db]
  (assoc db :current-user (extract-user (local-storage/get-item "shevek.access-token"))))

(defn page []
  (let [user (r/atom {})
        save #(dispatch :login user)
        shortcuts (kb-shortcuts :enter save)]
    (fn []
      [:div#login.ui.center.aligned.grid
       [:div.column
        [:h1.ui.blue.header
         [:i.cubes.icon]
         [:div.content "Shevek"
          [:div.sub.header "Data Warehouse Visualization System"]]]
        [:div.ui.segment (rpc/loading-class :logging-in)
         [:div.ui.form {:ref shortcuts :class (when (:error @user) "error")}
          [:div.field
           [:div.ui.left.icon.input
            [:i.user.icon]
            [text-input user :username {:placeholder (t :users/username) :auto-focus true}]]]
          [:div.field
           [:div.ui.left.icon.input
            [:i.lock.icon]
            [text-input user :password {:placeholder (t :users/password) :type "password"}]]]
          (when (:error @user)
            [:div.ui.error.message
             (t (:error @user))])
          [:button.ui.fluid.large.blue.primary.button {:on-click save} "Login"]]]]])))
