(ns reflow.db
  (:refer-clojure :exclude [get get-in])
  (:require [reagent.core :as r]
            [cljs.pprint :refer [pprint]]))

(defonce app-db (r/atom {}))

(defn get [k & [default]]
  (clojure.core/get @app-db k default))

(defn get-in [ks]
  (clojure.core/get-in @app-db ks))

(defn debug []
  [:pre (with-out-str (pprint @app-db))])
