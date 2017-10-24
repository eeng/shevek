(ns shevek.querying.aggregation
  (:require [shevek.lib.collections :refer [assoc-if-seq find-by]]
            [shevek.lib.druid-driver :as driver]
            [shevek.lib.time.ext :refer [plus-period]]
            [shevek.lib.dw.dims :refer [time-dimension? partition-splits row-split? col-split?]]
            [shevek.lib.collections :refer [detect]]
            [shevek.querying.conversion :refer [to-druid-query from-druid-results]]
            [com.rpl.specter :refer [setval must ALL]]))

(defn- send-query [dw q]
  (let [dq (to-druid-query q)
        dr (driver/send-query dw dq)]
    (from-druid-results q dq dr)))

(defn- dim-value [{:keys [name]} result]
  (result (keyword name)))

(defn- add-filter-for-dim [filters {:keys [granularity] :as dim} result]
  (let [value (dim-value dim result)]
    (if (time-dimension? dim)
      (setval [ALL (must :interval)]
              [value (plus-period value granularity)] filters)
      (conj filters (assoc dim :operator "is" :value value)))))

(defn- resolve-col-splits [dw {:keys [splits filters] :as q} grand-total]
  (let [[dim & dims] (filter col-split? splits)
        nested-query #(assoc q :splits %1 :filters (add-filter-for-dim filters dim %2))
        posible-values (when-not (time-dimension? dim)
                         (->> grand-total (mapcat :child-cols) (map (partial dim-value dim)) distinct))
        this-q (cond-> (assoc q :dimension dim)
                       (and posible-values (seq grand-total)) (update :filters conj (assoc dim :operator "include" :value posible-values)))]
    (when dim
      (->> (send-query dw this-q)
           (pmap #(assoc-if-seq % :child-cols (resolve-col-splits dw (nested-query dims %) (mapcat :child-cols grand-total))))))))

(defn- resolve-row-splits [dw {:keys [splits filters] :as q} grand-total]
  (let [[row-splits col-splits] (partition-splits splits)
        [dim & dims] row-splits
        nested-query #(assoc q :splits %1 :filters (add-filter-for-dim filters dim %2))
        nested-child-rows #(resolve-row-splits dw (nested-query (concat dims col-splits) %) grand-total)
        nested-child-cols #(resolve-col-splits dw (nested-query col-splits %) grand-total)]
    (when dim
      (->> (send-query dw (assoc q :dimension dim))
           (pmap #(assoc-if-seq % :child-rows (nested-child-rows %) :child-cols (nested-child-cols %)))
           doall))))

(defn- build-row-if-no-results [{:keys [measures]} results]
  (if (seq results)
    results
    (vector (zipmap (map (comp keyword :name) measures) (repeat 0)))))

(defn- resolve-grand-totals [dw q]
  (->> (send-query dw q)
       (pmap #(assoc-if-seq % :child-cols (resolve-col-splits dw q [])))
       (build-row-if-no-results q)))

(defn query [dw {:keys [cube totals splits] :as q}]
  (let [totals (or totals (empty? splits))
        grand-totals (if totals (resolve-grand-totals dw q) [])]
    (concat grand-totals
            (resolve-row-splits dw q grand-totals))))
