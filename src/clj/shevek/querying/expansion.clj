(ns shevek.querying.expansion
  (:require [shevek.lib.dw.dims :refer [find-dimension]]
            [shevek.lib.collections :refer [reverse-merge]]))

; TODO ver si las keys se podrian sacar direcamente de los schemas para no duplicar esa info
(defn expand-query
  "Take a query and the corresponding cube schema and expands the query parts with the schema information"
  [q {:keys [measures dimensions default-time-zone]}]
  (let [expand-measure #(-> (find-dimension % measures)
                            (select-keys [:name :expression]))
        expand-dimension #(-> (find-dimension (:name %) dimensions)
                              (select-keys [:name :column :extraction :type])
                              (merge %))]
    (-> q
        (update :measures (partial map expand-measure))
        (update :filters (partial map expand-dimension))
        (update :splits (partial map expand-dimension))
        (reverse-merge {:time-zone default-time-zone}))))
