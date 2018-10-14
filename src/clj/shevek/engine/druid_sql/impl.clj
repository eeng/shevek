(ns shevek.engine.druid-sql.impl
  (:require [shevek.engine.protocol :refer [Engine]]
            [shevek.engine.druid-native.metadata :as metadata]
            [shevek.engine.druid-native.raw :as raw]
            [shevek.driver.druid :as driver]
            [shevek.engine.druid-sql.query :as query]))

(defrecord DruidSQLEngine [driver]
  Engine

  (cubes [_]
    (driver/datasources driver))

  (time-boundary [_ cube]
    (metadata/time-boundary driver cube))

  (cube-metadata [_ cube]
    (metadata/cube-metadata driver cube))

  (designer-query [_ query cube]
    (query/execute-query driver query cube))

  (raw-query [_ query cube]
    (raw/execute-query driver query cube))

  (custom-query [_ query]
    (driver/send-query driver {:query query})))

(defn druid-sql-engine [driver]
  (DruidSQLEngine. driver))
