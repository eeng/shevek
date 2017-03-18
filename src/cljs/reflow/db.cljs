(ns reflow.db
  (:refer-clojure :exclude [get get-in])
  (:require [reagent.core :as r]
            [cljs.pprint :refer [pprint]]))

(defonce app-db (r/atom {}))

(defn- get-in-db [ks]
  (clojure.core/get-in @app-db ks))

(defn get-in [ks]
  @(r/track get-in-db ks))

(defn get [k & [default]]
  (or (get-in [k]) default))

(defn debug []
  [:pre (with-out-str (pprint @app-db))])
