(ns shevek.reports.api
  (:require [schema.core :as s]
            [shevek.reports.repository :as r]
            [shevek.reports.conversion :refer [viewer->report]]
            [shevek.schemas.viewer :refer [Viewer]]
            [shevek.db :refer [db]]))

(s/defn save-report [report viewer :- Viewer]
  (r/save-report db (merge report (viewer->report viewer))))

(s/defn delete-report [report]
  (r/delete-report db report))

(defn find-all []
  (r/find-reports db))
