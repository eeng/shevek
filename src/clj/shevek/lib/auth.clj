(ns shevek.lib.auth
  (:require [buddy.sign.jwt :as jwt]
            [buddy.auth :refer [throw-unauthorized]]
            [shevek.db :refer [db]]
            [shevek.users.repository :as users]
            [shevek.config :refer [config]]
            [bcrypt-clj.auth :refer [check-password]]
            [clj-time.core :as t]))

(def token-expiration (t/days 30))

(defn generate-token [{:keys [id] :as user}]
  (let [token (-> (select-keys user [:id :username :fullname :admin :email])
                  (assoc :exp (t/plus (t/now) token-expiration)))]
    (jwt/sign token (config :jwt-secret))))

(defn authenticate [db username password]
  (let [user (users/find-by-username db username)]
    (when (and user (check-password password (:password user)))
      user)))

(defn authenticate-and-generate-token [db {:keys [username password]}]
  (if-let [user (authenticate db username password)]
    {:token (->> user (users/update-sign-in-timestamps db) (generate-token))}
    {:error :invalid-credentials}))

(defn controller [{:keys [params]}]
  (let [{:keys [token] :as res} (authenticate-and-generate-token db params)]
    {:status (if token 201 401) :body res}))

(defn wrap-current-user
  "Store the user-id from the identity inserted by buddy, for easy access in the api functions"
  [handler]
  (fn [{:keys [identity] :as request}]
    (handler (assoc request
                    :user-id (when identity (:id identity))
                    :user (users/find-by-username db (:username identity))))))

(defn authorize [authorized?]
  (when-not authorized?
    (throw-unauthorized)))

#_(def token (:token (authenticate-and-generate-token db {:username "admin" :password "asdf654"})))
#_(jwt/unsign token (config :jwt-secret))
