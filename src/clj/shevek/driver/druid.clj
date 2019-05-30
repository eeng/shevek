(ns shevek.driver.druid)

(defprotocol DruidDriver
  (datasources [this])
  (send-query [this q]))
