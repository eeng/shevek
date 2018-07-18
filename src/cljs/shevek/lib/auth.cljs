(ns shevek.lib.auth
  (:require [shevek.reflow.db :as db]))

(defn current-user []
  (db/get :current-user))

(defn logged-in? []
  (some? (current-user)))

(defn admin? []
  (:admin (current-user)))
