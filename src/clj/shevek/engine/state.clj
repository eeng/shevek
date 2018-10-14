(ns shevek.engine.state
  (:require [mount.core :refer [defstate start stop]]
            [shevek.config :refer [config]]
            [shevek.engine.druid-native.impl :refer [druid-native-engine]]
            [shevek.engine.druid-sql.impl :refer [druid-sql-engine]]
            [shevek.driver.druid :refer [http-druid-driver]]))

(defstate dw
  :start (druid-native-engine (http-druid-driver (config :druid-uri))))
