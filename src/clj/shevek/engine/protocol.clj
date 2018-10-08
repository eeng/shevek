(ns shevek.engine.protocol)

(defprotocol Engine
  (cubes [this])
  (cube-metadata [this cube-name])
  (time-boundary [this cube-name])
  (designer-query [this query cube])
  (raw-query [this query cube])
  (custom-query [this query]))
