(ns shevek.makers
  (:require [schema-generators.complete :as c]
            [shevek.schema.repository :refer [save-cube]]
            [shevek.schemas.cube :refer [Cube]]
            [shevek.users.repository :refer [save-user]]
            [shevek.schemas.user :refer [User]]
            [shevek.reports.repository :refer [save-report]]
            [shevek.schemas.report :refer [Report]]
            [shevek.dashboards.repository :refer [save-dashboard]]
            [shevek.schemas.dashboard :refer [Dashboard]]
            [shevek.db :refer [db]]))

(def makers {Cube save-cube
             User save-user
             Report save-report
             Dashboard save-dashboard})

(def default-fields {Cube {:dimensions [{:name "__time"}]}})

(defn make
  ([schema] (make schema {}))
  ([schema args] (c/complete (merge (default-fields schema) args) schema)))

(defn make!
  ([schema] (make! schema {}))
  ([schema args]
   (let [saver (makers schema)]
     (assert saver (str "You need to define a save function (in makers) for the schema: " (meta schema)))
     (->> (make schema args) (saver db)))))
