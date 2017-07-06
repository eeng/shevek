(ns shevek.lib.auth
  (:require [buddy.sign.jwt :as jwt]
            [shevek.db :refer [db]]
            [shevek.users.repository :as users]
            [shevek.config :refer [config]]
            [bcrypt-clj.auth :refer [check-password]]
            [clj-time.core :as t]
            [shevek.web.pages :as pages]
            [ring.util.response :refer [redirect]]))

(defn- generate-token [{:keys [username]}]
  (jwt/sign {:username username :exp (t/plus (t/now) (t/days 1))} (config :jwt-secret)))

(defn authenticate [db {:keys [username password]}]
  (let [user (users/find-by-username db username)]
    (if (and user (check-password password (:password user)))
      {:token (generate-token user)}
      {:error :invalid-credentials})))

(defn login [params]
  (let [{:keys [token] :as res} (authenticate db params)]
    (if token
      (redirect (str "/")) ; TODO poner el token en la cookie
      (pages/login params))))

#_(def token (:token (authenticate db {:username "emma" :password "asdf654"})))
#_(jwt/unsign token (config :jwt-secret))
