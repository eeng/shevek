(ns shevek.reflow.db
  (:refer-clojure :exclude [get get-in])
  (:require [reagent.core :as r]
            [cljs.pprint :refer [pprint]]))

(defonce app-db (r/atom {}))

(defn- get-in-db [ks default]
  (clojure.core/get-in @app-db ks default))

(defn get-in [ks & [default]]
  @(r/track get-in-db ks default))

(defn get [k & [default]]
  (get-in [k] default))

(defn debug []
  [:pre (with-out-str (pprint @app-db))])
