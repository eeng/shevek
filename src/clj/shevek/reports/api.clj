(ns shevek.reports.api
  (:require [schema.core :as s]
            [shevek.reports.repository :as r]
            [shevek.db :refer [db]]))

(s/defn save-report [report]
  (r/save-report db report))

(s/defn delete-report [report]
  (r/delete-report db report))

(defn find-all []
  (r/find-reports db))
