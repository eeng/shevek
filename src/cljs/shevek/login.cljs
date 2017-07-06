(ns shevek.login
  (:require-macros [reflow.macros :refer [defevh]])
  (:require [reagent.core :as r]
            [shevek.i18n :refer [t]]
            [reflow.db :as db]
            [reflow.core :refer [dispatch]]
            [shevek.rpc :as rpc]
            [shevek.components :refer [text-input focused kb-shortcuts]]
            [ajax.core :refer [POST]]
            [clojure.string :as str]
            [goog.crypt.base64 :as b64]))

(defn current-user []
  (db/get :current-user))

(defn logged-in? []
  (some? (current-user)))

(defn extract-user [token]
  (as-> token $
        (str/split $ #"\.")
        (second $)
        (b64/decodeString $)
        (.parse js/JSON $)
        (js->clj $ :keywordize-keys true)))

(defevh :user/login-successful [db {:keys [token]}]
  (-> (assoc db :current-user (extract-user token))
      (rpc/loaded :logging-in)))

(defevh :user/login-failed [db user]
  (swap! user assoc :error :users/invalid-credentials)
  (rpc/loaded db :logging-in))

(defevh :user/login [db user]
  (swap! user dissoc :error)
  (POST "/login" {:params (dissoc @user :error)
                  :handler #(dispatch :user/login-successful %)
                  :error-handler #(dispatch :user/login-failed user)})
  (rpc/loading db :logging-in))

(defn page []
  (let [user (r/atom {})
        save #(dispatch :user/login user)
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
