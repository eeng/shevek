(ns shevek.reports.repository
  (:require [schema.core :as s]
            [shevek.schemas.report :refer [Report]]
            [monger.collection :as mc]))

(s/defn save-report [db report :- Report]
  (mc/save-and-return db "reports" report))
