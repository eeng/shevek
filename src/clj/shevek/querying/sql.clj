(ns shevek.querying.sql
  (:require [shevek.lib.druid-driver :refer [send-query]]))

(defn query [dw sql]
  (send-query dw {:query sql}))

#_(query shevek.dw/dw "SELECT sum(added) from wikiticker")
