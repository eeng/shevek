(ns shevek.engines.engine)

(defprotocol DwEngine
  (cubes [this] "Returns a list of maps where each map represents a cube")
  (cube [this name] "Returns a map with metadata (dimensions, measures, etc) about the cube selected")
  (query [this q]))
