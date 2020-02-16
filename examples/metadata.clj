(ns examples.metadata
  (:require [shevek.engine.state :refer [dw]]
            [shevek.engine.protocol :as e]
            [shevek.config :refer [config]]
            [shevek.schema.manager :as m]))

(comment
  (e/cubes dw)
  (e/time-boundary dw "wikipedia")
  (e/cube-metadata dw "wikipedia")

  (config :cubes)

  (m/discover-cubes dw))
