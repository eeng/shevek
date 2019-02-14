(ns shevek.pages.dashboards.helpers
  (:require [shevek.domain.auth :refer [mine?]]))

(defn slave? [{:keys [master-id]}]
  (some? master-id))

(def master? (comp not slave?))

(defn modifiable? [dashboard]
  (and (mine? dashboard) (master? dashboard)))
