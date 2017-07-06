(ns shevek.lib.auth
  (:require [buddy.sign.jwt :as jwt]
            [shevek.db :refer [db]]
            [shevek.users.repository :as users]
            [shevek.config :refer [config]]
            [bcrypt-clj.auth :refer [check-password]]))

(defn authenticate [db {:keys [username password]}]
  (let [user (users/find-by-username db username)]
    (if (and user (check-password password (:password user)))
      {:status :success :token (jwt/sign {:username username} (config :jwt-secret))}
      {:status :failure :error :invalid-credentials})))

(defn login [params]
  (let [{:keys [status] :as res} (authenticate db params)]
    {:status (if (= status :success) 201 401) :body res}))
