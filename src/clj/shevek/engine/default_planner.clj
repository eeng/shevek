(ns shevek.engine.default-planner
  (:require [shevek.querying.expansion :refer [expand-query]]
            [shevek.lib.collections :refer [assoc-if-seq find-by]]
            [shevek.domain.dimension :refer [partition-splits time-dimension? row-split? col-split?]]
            [shevek.engine.protocol :refer [resolve-expanded-query]]
            [com.rpl.specter :refer [setval must ALL]]
            [shevek.lib.time.ext :refer [plus-period]]))

(defn dimension-value [{:keys [name]} result]
  (->> name keyword (get result)))

(defn- add-filter-for-dim [filters {:keys [granularity] :as dim} result]
  (let [value (dimension-value dim result)]
    (if (time-dimension? dim)
      (setval [ALL (must :interval)]
              [value (plus-period value granularity)] filters)
      (conj filters (assoc dim :operator "is" :value value)))))

(defn- resolve-col-splits [engine {:keys [splits filters] :as q} grand-total]
  (let [[dim & dims] (filter col-split? splits)
        nested-query #(assoc q :splits %1 :filters (add-filter-for-dim filters dim %2))
        posible-values (when-not (time-dimension? dim)
                         (->> grand-total (mapcat :child-cols) (map (partial dimension-value dim)) distinct))
        this-q (cond-> (assoc q :dimension dim)
                       (and posible-values (seq grand-total)) (update :filters conj (assoc dim :operator "include" :value posible-values)))]
    (when dim
      (->> (resolve-expanded-query engine this-q)
           (pmap #(assoc-if-seq % :child-cols (resolve-col-splits engine (nested-query dims %) (mapcat :child-cols grand-total))))))))

(defn- resolve-row-splits [engine {:keys [splits filters] :as q} grand-total]
  (let [[row-splits col-splits] (partition-splits splits)
        [dim & dims] row-splits
        nested-query #(assoc q :splits %1 :filters (add-filter-for-dim filters dim %2))
        nested-child-rows #(resolve-row-splits engine (nested-query (concat dims col-splits) %) grand-total)
        nested-child-cols #(resolve-col-splits engine (nested-query col-splits %) grand-total)]
    (when dim
      (->> (resolve-expanded-query engine (assoc q :dimension dim))
           (pmap #(assoc-if-seq % :child-rows (nested-child-rows %) :child-cols (nested-child-cols %)))
           doall))))

(defn- build-row-if-no-results [{:keys [measures]} results]
  (if (seq results)
    results
    (vector (zipmap (map (comp keyword :name) measures) (repeat 0)))))

(defn- resolve-grand-totals [engine q]
  (->> (resolve-expanded-query engine q)
       (pmap #(assoc-if-seq % :child-cols (resolve-col-splits engine q [])))
       (build-row-if-no-results q)))

(defn execute [engine query cube-schema]
  (let [{:keys [cube totals splits] :as q} (expand-query query cube-schema)
        totals (or totals (empty? splits))
        grand-totals (if totals (resolve-grand-totals engine q) [])]
    (concat grand-totals (resolve-row-splits engine q grand-totals))))
