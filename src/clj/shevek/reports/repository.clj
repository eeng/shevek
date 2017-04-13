(ns shevek.reports.repository
  (:require [schema.core :as s]
            [shevek.schemas.report :refer [Report]]
            [monger.collection :as mc]
            [monger.query :as mq]))

(s/defn save-report [db report :- Report]
  (mc/save-and-return db "reports" report))

(s/defn delete-report [db {:keys [_id]}]
  (mc/remove-by-id db "reports" _id)
  true)

; TODO quizas convenga editarlos por fecha de creacion o ultimo acceso (pero aca habria q ir actualizando esta fecha)
(defn find-reports [db]
  (mq/with-collection db "reports"
    (mq/sort {:name 1})))
