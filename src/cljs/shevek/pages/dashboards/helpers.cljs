(ns shevek.pages.dashboards.helpers
  (:require [shevek.domain.auth :refer [current-user]]))

(defn mine? [{:keys [owner-id]}]
  (= owner-id (current-user :id)))

(defn slave? [{:keys [master-id]}]
  (some? master-id))

(def master? (comp not slave?))

(defn modifiable? [dashboard]
  (and (mine? dashboard) (master? dashboard)))
