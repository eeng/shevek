(ns shevek.dw
  (:require [shevek.engines.engine :as e]
            [shevek.engines.memory]
            [shevek.engines.druid])
  (:import [shevek.engines.memory InMemoryEngine]
           [shevek.engines.druid DruidEngine]))

#_(def engine (InMemoryEngine.))
(def engine (DruidEngine. "http://kafka:8082"))

(defn cubes []
  (e/cubes engine))

(defn cube [name]
  (e/cube engine name))

(defn query [q]
  (e/query engine q))

(defn max-time [q]
  (e/max-time engine q))
