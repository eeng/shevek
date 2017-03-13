(ns pivot.dw
  (:require [pivot.engines.engine :as e]
            [pivot.engines.memory]
            [pivot.engines.druid])
  (:import [pivot.engines.memory InMemoryEngine]
           [pivot.engines.druid DruidEngine]))

(defn cubes []
  #_(e/cubes (InMemoryEngine.))
  (e/cubes (DruidEngine. "http://kafka:8082")))
