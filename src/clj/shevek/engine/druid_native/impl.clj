(ns shevek.engine.druid-native.impl
  (:require [shevek.engine.protocol :refer [Engine]]
            [shevek.engine.druid-native.metadata :as metadata]
            [shevek.engine.druid-native.planner :as planner]
            [shevek.engine.druid-native.raw :as raw]
            [shevek.driver.druid :as driver]))

(defrecord DruidNativeEngine [driver]
  Engine

  (cubes [_]
    (driver/datasources driver))

  (time-boundary [_ cube]
    (metadata/time-boundary driver cube))

  (cube-metadata [_ cube]
    (metadata/cube-metadata driver cube))

  (designer-query [_ query cube]
    (planner/execute-query driver query cube))

  (raw-query [_ query cube]
    (raw/execute-query driver query cube))

  (custom-query [_ query]
    (driver/send-query driver {:query query})))

(defn druid-native-engine [driver]
  (DruidNativeEngine. driver))
