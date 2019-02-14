(ns shevek.domain.auth
  (:require [shevek.reflow.db :as db]))

(defn current-user [& keys]
  (db/get-in (concat [:current-user] keys)))

(defn logged-in? []
  (some? (current-user)))

(defn admin? []
  (current-user :admin))

(defn mine? [{:keys [owner-id]}]
  (= owner-id (current-user :id)))
