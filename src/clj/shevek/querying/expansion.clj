(ns shevek.querying.expansion
  (:require [shevek.lib.dw.dims :refer [find-dimension time-dimension?]]
            [shevek.lib.collections :refer [reverse-merge]]
            [shevek.lib.period :refer [to-interval normalize-interval]]
            [shevek.lib.time :refer [to-iso8601]]
            [com.rpl.specter :refer [transform ALL]]))

(defn- effective-interval [{:keys [period interval]} max-time]
  (let [interval (if period
                   (to-interval period max-time)
                   (normalize-interval interval))]
    (map to-iso8601 interval)))

(defn- relative-to-absolute-time [q max-time]
  (transform [:filters ALL time-dimension?]
             #(-> (assoc % :interval (effective-interval % max-time))
                  (dissoc :period))
             q))

; TODO ver si las keys se podrian sacar direcamente de los schemas para no duplicar esa info
(defn expand-query
  "Take a query and the corresponding cube schema and expands the query parts with the schema information"
  [q {:keys [measures dimensions default-time-zone max-time]}]
  (let [expand-measure #(-> (find-dimension % measures)
                            (select-keys [:name :expression]))
        expand-dimension #(-> (find-dimension (:name %) dimensions)
                              (select-keys [:name :column :extraction :type])
                              (merge %))]
    (-> q
        (update :measures (partial map expand-measure))
        (update :filters (partial map expand-dimension))
        (update :splits (partial map expand-dimension))
        (reverse-merge {:time-zone default-time-zone})
        (relative-to-absolute-time max-time))))
