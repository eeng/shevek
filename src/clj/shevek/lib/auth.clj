(ns shevek.lib.auth
  (:require [buddy.auth :refer [throw-unauthorized]]
            [shevek.db :refer [db]]
            [shevek.users.repository :as users]
            [bcrypt-clj.auth :refer [check-password]]))

(defn authenticate [db username password]
  (let [user (users/find-by-username db username)]
    (when (and user (check-password password (:password user)))
      user)))

(defn authenticate-and-update-timestamps [db {:keys [username password]}]
  (some->> (authenticate db username password)
           (users/update-sign-in-timestamps db)))

(defn public-user-data [user]
  (select-keys user [:id :username :fullname :admin :email]))

(defn login [{:keys [params session]}]
  (if-let [user (authenticate-and-update-timestamps db params)]
    {:status 201
     :body {:user (public-user-data user)}
     :session (assoc session :identity (:id user))}
    {:status 401
     :body {:error :invalid-credentials}}))

(defn logout [_]
  {:status 200 :session nil})

; Middleware

(defn wrap-current-user
  "Store the user-id from the identity inserted by buddy, for easy access in the api functions"
  [handler]
  (fn [{:keys [identity] :as request}]
    (handler (assoc request
                    :user-id identity
                    :user (users/find-by-id db identity)))))

; Authorization functions

(defn authorize [authorized?]
  (when-not authorized?
    (throw-unauthorized)))

(defn authorize-to-owner [{:keys [user-id]} {:keys [owner-id]}]
  (authorize (= user-id owner-id)))
