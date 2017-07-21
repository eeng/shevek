(ns shevek.users.api
  (:require [shevek.users.repository :as r]
            [shevek.db :refer [db]]
            [shevek.lib.auth :refer [authenticate generate-token]]))

(defn find-all [_]
  (r/find-users db))

(defn save [_ user]
  (r/save-user db user))

(defn delete [_ user]
  (r/delete-user db user))

(defn save-account [{:keys [identity]} {:keys [current-password] :as new-fields}]
  (if-let [user (authenticate db (:username identity) current-password)]
    (as-> (dissoc user :password) user
          (merge user (select-keys new-fields [:username :fullname :email :password]))
          (r/save-user db user)
          {:token (generate-token user)})
    {:error :invalid-current-password}))
