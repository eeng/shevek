(ns reflow.db
  (:refer-clojure :exclude [get get-in])
  (:require [reagent.core :as r]))

(defonce app-db (r/atom {}))

(defn get [k & [default]]
  (clojure.core/get @app-db k default))

(defn get-in [ks]
  (clojure.core/get-in @app-db ks))

(defn debug-db []
  [:pre (with-out-str (cljs.pprint/pprint @app-db))])