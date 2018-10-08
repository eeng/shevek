(ns shevek.engine.state
  (:require [mount.core :refer [defstate start stop]]
            [shevek.config :refer [config]]
            [shevek.engine.druid :refer [druid-engine]]
            [shevek.engine.druid.driver :refer [http-druid-driver]]))

(defstate dw
  :start (druid-engine (http-druid-driver (config :druid-uri))))
