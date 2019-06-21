(ns shevek.pages.login
  (:require [reagent.core :as r]
            [ajax.core :refer [POST]]
            [shevek.i18n :refer [t]]
            [shevek.reflow.db :as db]
            [shevek.reflow.core :refer [dispatch] :refer-macros [defevh]]
            [shevek.rpc :as rpc]
            [shevek.components.form :refer [text-input input-field]]
            [shevek.components.shortcuts :refer [shortcuts]]
            [shevek.navigation :refer [navigate]]
            [shevek.components.notification :refer [notify]]))

(defevh :sessions/login-successful [db {:keys [user]}]
  (-> (assoc db :current-user user)
      (rpc/loaded :logging-in)))

(defevh :sessions/login-failed [db]
  (notify (t :users/invalid-credentials) :type :error)
  (-> (js/$ "#form-container") (.transition "shake" #js {:duration ".6s" :queue false}))
  (rpc/loaded db :logging-in))

(defevh :sessions/login [db user]
  (POST "/login" {:params @user
                  :handler #(dispatch :sessions/login-successful %)
                  :error-handler (fn [{:keys [status] :as response}]
                                   (if (= status 401)
                                     (dispatch :sessions/login-failed)
                                     (dispatch :errors/from-server response)))})
  (rpc/loading db :logging-in))

(defn- reset-db-and-go-to-login [db]
  (navigate "/")
  (select-keys db [:preferences :page :initialized]))

(defevh :sessions/logout [db]
  (POST "/logout" :handler identity)
  (reset-db-and-go-to-login db))

(defevh :sessions/expired [db]
  (notify (t :users/session-expired) :type :info)
  (reset-db-and-go-to-login db))

(defevh :sessions/restored [db user]
  (assoc db :current-user user :initialized true))

(defevh :sessions/restore [db]
  (rpc/call "users/me"
            :handler #(dispatch :sessions/restored %)
            :error-handler #(dispatch :sessions/restored nil)))

(defn- login-form []
  (let [user (r/atom {})
        login #(dispatch :sessions/login user)]
    (fn []
      [shortcuts {:enter login}
       [:div.ui.form
        [input-field user :username {:placeholder (t :users/username) :auto-focus true :icon "user"}]
        [input-field user :password {:placeholder (t :users/password) :type "password" :icon "lock"}]
        [:button.ui.fluid.large.blue.primary.button {:on-click login} "Login"]]])))

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
