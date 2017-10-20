(ns shevek.querying.expansion
  (:require [shevek.lib.dw.dims :refer [find-dimension time-dimension?]]
            [shevek.lib.collections :refer [reverse-merge]]
            [shevek.lib.period :refer [to-interval normalize-interval]]
            [shevek.lib.time :as t]
            [com.rpl.specter :refer [transform ALL]]))

(defn- effective-interval [{:keys [period interval time-zone]} max-time]
  (let [interval (if period
                   (to-interval period max-time)
                   (normalize-interval interval))]
    (map t/to-iso8601 interval)))

(defn- relative-to-absolute-time [q max-time]
  (transform [:filters ALL time-dimension?]
             #(-> (assoc % :interval (effective-interval % max-time))
                  (dissoc :period))
             q))

(defn- expand-dim [dim dimensions schema-keys-to-keep]
  (let [dim (if (string? dim) {:name dim} dim)]
    (-> (find-dimension (:name dim) dimensions)
        (select-keys schema-keys-to-keep)
        (merge dim))))

(defn expand-query
  "Take a query and the corresponding cube schema and expands it with some schema information necessary to execute the query"
  [q {:keys [measures dimensions default-time-zone max-time]}]
  (-> q
      (update :measures (partial map #(expand-dim % measures [:expression])))
      (update :filters (partial map #(expand-dim % dimensions [:column :extraction])))
      (update :splits (partial map #(cond-> (expand-dim % dimensions [:column :extraction])
                                            (:sort-by %) (update :sort-by expand-dim (concat dimensions measures) [:type :expression]))))
      (reverse-merge {:time-zone (or default-time-zone (t/system-time-zone))})
      (relative-to-absolute-time max-time)))
