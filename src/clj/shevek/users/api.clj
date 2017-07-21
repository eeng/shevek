(ns shevek.users.api
  (:require [shevek.users.repository :as r]
            [shevek.db :refer [db]]
            [shevek.lib.auth :refer [generate-token-response]]))

(defn find-all [_]
  (r/find-users db))

(defn save [_ user]
  (r/save-user db user))

(defn delete [_ user]
  (r/delete-user db user))

(defn save-account [{:keys [user-id]} new-details]
  (as-> (r/find-by-id db user-id) user
        (dissoc user :password)
        (merge user (select-keys new-details [:username :fullname :email :password]))
        (r/save-user db user)
        (generate-token-response user)))
