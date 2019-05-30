(ns shevek.env
  (:require [shevek.config :refer [config]]))

(defn env []
  (config :env "development"))

(defn development? []
  (= (env) "development"))

(defn production? []
  (= (env) "production"))

(defn test? []
  (= (env) "test"))

#_(env)
