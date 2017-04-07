(ns shevek.users.api
  (:require [shevek.users.repository :as r]
            [shevek.db :refer [db]]))

(defn find-all []
  (r/find-all db))
