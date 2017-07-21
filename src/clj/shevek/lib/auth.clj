(ns shevek.lib.auth
  (:require [buddy.sign.jwt :as jwt]
            [shevek.db :refer [db]]
            [shevek.users.repository :as users]
            [shevek.config :refer [config]]
            [shevek.lib.mongodb :refer [oid]]
            [bcrypt-clj.auth :refer [check-password]]
            [clj-time.core :as t]))

(def token-expiration (t/days 1))

(defn generate-token [{:keys [_id] :as user}]
  (let [token (-> (dissoc user :_id :password)
                  (assoc :id (str _id) :exp (t/plus (t/now) token-expiration)))]
    (jwt/sign token (config :jwt-secret))))

(defn generate-token-response [user]
  {:token (generate-token user)})

(defn authenticate [db {:keys [username password]}]
  (let [user (users/find-by-username db username)]
    (if (and user (check-password password (:password user)))
      (generate-token-response user)
      {:error :invalid-credentials})))

(defn controller [{:keys [params]}]
  (let [{:keys [token] :as res} (authenticate db params)]
    {:status (if token 201 401) :body res})) ;

(defn wrap-current-user
  "Store the user-id from the identity inserted by buddy, for easy access in the api functions"
  [handler]
  (fn [{:keys [identity] :as request}]
    (handler (assoc request :user-id (when identity (oid (:id identity)))))))

#_(def token (:token (authenticate db {:username "emma" :password "asdf654"})))
#_(jwt/unsign token (config :jwt-secret))
