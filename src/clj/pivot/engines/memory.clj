(ns pivot.engines.memory
  (:require [pivot.engines.engine :refer [DwEngine]]))

(def test-cubes
  [{:name "vtol_stats"
    :title "VTOL Stats"
    :description "Estadísticas de uso del sistema VTOL."
    :dimensions [{:name "controller" :title "Controller" :cardinality 123 :description "Controller del request"}
                 {:name "action" :title "Action" :cardinality 43 :description "Rails action"}]
    :measures [{:name "requests"}
               {:name "duration"}]}
   {:name "eventos_pedidos"
    :title "Eventos de Pedidos"
    :description "Info sobre eventos generados por el ruteo de pedidos."
    :dimensions [{:name "evento"}
                 {:name "oficina"}
                 {:name "adicion" :type "LONG"}]
    :measures [{:name "pedidos"}
               {:name "usuarios"}]}
   {:name "facturacion"
    :dimensions [{:name "__time"}]
    :measures []}])

(defrecord InMemoryEngine []
  DwEngine
  (cubes [_] test-cubes))
