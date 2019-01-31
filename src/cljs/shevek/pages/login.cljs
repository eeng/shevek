(ns shevek.pages.login
  (:require [reagent.core :as r]
            [shevek.i18n :refer [t]]
            [shevek.reflow.db :as db]
            [shevek.reflow.core :refer [dispatch] :refer-macros [defevh]]
            [shevek.rpc :as rpc]
            [shevek.components.form :refer [text-input input-field kb-shortcuts]]
            [shevek.lib.local-storage :as local-storage]
            [ajax.core :refer [POST]]
            [shevek.navigation :refer [navigate]]
            [cljsjs.jwt-decode]
            [shevek.components.notification :refer [notify]]))

(defn extract-user [token]
  (when token
    (try
      (js->clj (js/jwt_decode token) :keywordize-keys true)
      (catch js/Error _ {}))))

(defevh :login-successful [db {:keys [token]}]
  (local-storage/set-item! "access-token" token)
  (-> (assoc db :current-user (extract-user token))
      (rpc/loaded :logging-in)))

(defevh :login-failed [db]
  (notify (t :users/invalid-credentials) :type :error)
  (-> (js/$ "#form-container") (.transition "shake" #js {:duration ".6s" :queue false}))
  (rpc/loaded db :logging-in))

(defevh :login [db user]
  (POST "/login" {:params @user
                  :handler #(dispatch :login-successful %)
                  :error-handler (fn [{:keys [status] :as response}]
                                   (if (= status 401)
                                     (dispatch :login-failed)
                                     (dispatch :server-error response)))})
  (rpc/loading db :logging-in))

(defevh :logout [db]
  (local-storage/remove-item! "access-token")
  (navigate "/")
  (select-keys db [:preferences :page :initialized]))

(defevh :session-expired [db]
  (notify (t :users/session-expired) :type :info)
  (dispatch :logout))

(defevh :sessions/user-restored [db]
  (assoc db :current-user (extract-user (local-storage/get-item "access-token")) :initialized true))

(defn- login-form []
  (let [user (r/atom {})
        login #(dispatch :login user)
        shortcuts (kb-shortcuts :enter login)]
    (fn []
      [:div.ui.form {:ref shortcuts}
       [input-field user :username {:placeholder (t :users/username) :auto-focus true :icon "user"}]
       [input-field user :password {:placeholder (t :users/password) :type "password" :icon "lock"}]
       [:button.ui.fluid.large.blue.primary.button {:on-click login} "Login"]])))

(defn page []
  [:div#login.ui.center.aligned.grid
   [:div.column
    [:h1.ui.blue.header
     [:img {:src "public/images/logo.png"}]
     [:div.content "Shevek"
      [:div.sub.header "Data Warehouse Visualization System"]]]
    [:div#form-container
     [:div.ui.segment (rpc/loading-class :logging-in)
      [login-form]]]]])
