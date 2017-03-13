(ns pivot.dw
  (:require [pivot.druid :as druid]))

#_(defn cubes [])
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
    :measures []}]

; TODO Ver como manejar este druid/broker, no me convence como esta ahora
(defn- with-dimensions-and-measures [{:keys [name] :as datasource}]
  (assoc datasource
         :dimensions (druid/dimensions druid/broker name)
         :measures (druid/metrics druid/broker name)))

(defn cubes []
  (map with-dimensions-and-measures (druid/datasources druid/broker)))
