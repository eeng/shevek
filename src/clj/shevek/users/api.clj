(ns shevek.users.api
  (:require [shevek.users.repository :as r]
            [shevek.db :refer [db]]
            [shevek.lib.auth :refer [authenticate authorize generate-token]]
            [shevek.schemas.user :refer [admin?]]))

(defn find-all [{:keys [user]}]
  (authorize (admin? user))
  (r/find-users db))

(defn save [_ user]
  (r/save-user db user))

(defn delete [_ id]
  (r/delete-user db id))

(defn save-account [{:keys [user]} {:keys [current-password] :as new-fields}]
  (if-let [user (authenticate db (:username user) current-password)]
    (as-> (dissoc user :password) user
          (merge user (select-keys new-fields [:fullname :email :password]))
          (r/save-user db user)
          {:token (generate-token user)})
    {:error :invalid-current-password}))
