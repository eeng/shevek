(ns pivot.dw
  (:require [pivot.engines.engine :as e]
            [pivot.engines.memory]
            [pivot.engines.druid])
  (:import [pivot.engines.memory InMemoryEngine]
           [pivot.engines.druid DruidEngine]))

(def engine (InMemoryEngine.))
#_(def engine (DruidEngine. "http://kafka:8082"))

(defn cubes []
  (e/cubes engine))

(defn cube [name]
  (e/cube engine name))
