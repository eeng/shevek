(ns pivot.engines.engine)

(defprotocol DwEngine
  (cubes [this])
  (query [this q]))
