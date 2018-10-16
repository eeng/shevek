(ns shevek.engine.druid-sql.impl
  (:require [shevek.engine.protocol :refer [Engine]]
            [shevek.engine.default-planner :as default-planner]
            [shevek.engine.druid-native.metadata :as metadata]
            [shevek.engine.druid-native.raw :as raw]
            [shevek.engine.druid-sql.solver :as solver]
            [shevek.driver.druid :as driver]))

(defrecord DruidSQLEngine [driver]
  Engine

  (cubes [_]
    (driver/datasources driver))

  (time-boundary [_ cube]
    (metadata/time-boundary driver cube))

  (cube-metadata [_ cube]
    (metadata/cube-metadata driver cube))

  (designer-query [this query cube]
    (default-planner/execute this query cube))

  (resolve-expanded-query [_ query]
    (solver/resolve-expanded-query driver query))

  (raw-query [_ query cube]
    (raw/execute-query driver query cube))

  (custom-query [_ query]
    (driver/send-query driver {:query query})))

(defn druid-sql-engine [driver]
  (DruidSQLEngine. driver))
