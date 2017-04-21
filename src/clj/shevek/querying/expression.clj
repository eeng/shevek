(ns shevek.querying.expression
  (:require [clojure.core.match :refer [match]]
            [shevek.lib.collections :refer [includes?]]))

(defn measure->druid [measure])

(def aggregator-types {'sum "doubleSum" 'count-distinct "hyperUnique"})

(defn eval-constant [value aggregators-so-far]
  [{:type "constant" :value value} aggregators-so-far])

(defn eval-aggregation [agg-fn field aggregators-so-far]
  (let [field (-> field str (subs 1))
        temp-field (str "t" (count aggregators-so-far) "!" field)
        field-type (aggregator-types agg-fn)]
    [{:type "fieldAccess" :fieldName temp-field}
     (conj aggregators-so-far {:fieldName field :type field-type :name temp-field})]))

(declare eval-expression*)

(defn eval-arithmetic [operator args aggregators-so-far]
  (let [acumulator (fn [[fields aggregators-so-far] arg]
                     (let [[post-agg new-aggregators] (eval-expression* arg aggregators-so-far)]
                       [(conj fields post-agg) new-aggregators]))
        [fields aggregators] (reduce acumulator [[] aggregators-so-far] args)]
    [{:type "arithmetic" :fn (str operator) :fields fields} aggregators]))

(defn- aggregation? [f]
  (includes? (keys aggregator-types) f))

(defn- arithmetic-operator? [op]
  (includes? ['/ '* '+ '- 'quotient] op))

(defn eval-expression* [e aggregators-so-far]
  (match e
    (value :guard number?) (eval-constant value aggregators-so-far)
    ([(a :guard aggregation?) field] :seq) (eval-aggregation a field aggregators-so-far)
    ([(op :guard arithmetic-operator?) & args] :seq) (eval-arithmetic op args aggregators-so-far)))

(defn eval-expression
  [e]
  "Takes a measure expression and returns a vector of the corresponding post-aggregator and the required aggregators"
  (eval-expression* (read-string e) []))
