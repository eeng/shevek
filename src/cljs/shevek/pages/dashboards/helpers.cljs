(ns shevek.pages.dashboards.helpers
  (:require [shevek.domain.auth :refer [mine?]]))

(defn modifiable? [dashboard]
  (mine? dashboard))
