(ns shevek.dw2
  (:require [shevek.config :refer [config]]
            [shevek.lib.druid-driver :as druid]
            [mount.core :refer [defstate]]))

(defstate dw :start (druid/connect (config :druid-uri)))
