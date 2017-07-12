(ns shevek.users.api
  (:require [shevek.users.repository :as r]
            [shevek.db :refer [db]]))

(defn find-all [_]
  (r/find-users db))

(defn save [_ user]
  (r/save-user db user))

(defn delete [_ user]
  (r/delete-user db user))
