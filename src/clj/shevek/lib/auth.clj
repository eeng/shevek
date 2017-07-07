(ns shevek.lib.auth
  (:require [buddy.sign.jwt :as jwt]
            [shevek.db :refer [db]]
            [shevek.users.repository :as users]
            [shevek.config :refer [config]]
            [buddy.hashers :as hashers]
            [clj-time.core :as t]))

(def token-expiration (t/days 1))

(defn- generate-token [user]
  (let [token (-> (dissoc user :_id :password)
                  (assoc :exp (t/plus (t/now) token-expiration)))]
    (jwt/sign token (config :jwt-secret))))

(defn authenticate [db {:keys [username password]}]
  (let [user (users/find-by-username db username)]
    (if (and user (hashers/check password (:password user)))
      {:token (generate-token user)}
      {:error :invalid-credentials})))

(defn login [params]
  (let [{:keys [token] :as res} (authenticate db params)]
    {:status (if token 201 401) :body res})) ;

#_(def token (:token (authenticate db {:username "emma" :password "asdf654"})))
#_(jwt/unsign token (config :jwt-secret))
