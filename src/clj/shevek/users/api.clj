(ns shevek.users.api
  (:require [shevek.users.repository :as r]
            [shevek.db :refer [db]]
            [shevek.lib.auth :refer [authenticate authorize public-user-data]]
            [shevek.schemas.user :refer [admin?]]))

(defn find-all [{:keys [user]}]
  (authorize (admin? user))
  (r/find-users db))

(defn save [{:keys [user]} user-to-save]
  (authorize (admin? user))
  (r/save-user db user-to-save))

(defn delete [{:keys [user]} id]
  (authorize (admin? user))
  (r/delete-user db id))

(defn save-account [{:keys [user]} {:keys [current-password] :as new-fields}]
  (if-let [user (authenticate db (:username user) current-password)]
    (as-> (dissoc user :password) user
          (merge user (select-keys new-fields [:fullname :email :password]))
          (r/save-user db user)
          {:user (public-user-data user)})
    {:error :invalid-current-password}))

(defn me [{:keys [user]}]
  (public-user-data user))
