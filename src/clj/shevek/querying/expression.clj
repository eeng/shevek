(ns shevek.querying.expression
  (:require [clojure.core.match :refer [match]]
            [clojure.string :refer [starts-with?]]
            [shevek.lib.collections :refer [includes?]]))

(def aggregator-types {'sum "doubleSum" 'count-distinct "hyperUnique"})

(defn- field-ref->field [field-ref]
  (-> field-ref str (subs 1)))

(defn- build-aggregator [name agg-fn field-ref]
  {:type (aggregator-types agg-fn)
   :fieldName (field-ref->field field-ref)
   :name name})

(defn- condition->filter [[_ field-ref value]]
  {:type "selector"
   :dimension (field-ref->field field-ref)
   :value value})

(defn- build-filtered-aggregator [field condition aggregator]
  {:type "filtered"
   :filter (condition->filter condition)
   :aggregator aggregator
   :name field})

(defn- simple-aggregation? [f]
  (includes? (keys aggregator-types) f))

(defn- aggregation? [f]
  (includes? (conj (keys aggregator-types) 'where) f))

(defn- arithmetic-operator? [op]
  (includes? ['/ '* '+ '- 'quotient] op))

(defn- eval-aggregator [name expression]
  (match expression
    ([(agg-fn :guard simple-aggregation?) field-ref] :seq) (build-aggregator name agg-fn field-ref)
    (['where condition subexp] :seq) (build-filtered-aggregator name condition (eval-aggregator name subexp))))

(defn- build-constant-postagg [value aggregators-so-far]
  [aggregators-so-far {:type "constant" :value value}])

(defn- build-field-access-postagg [expression aggregators-so-far]
  (let [temp-name (str "_t" (count aggregators-so-far))
        new-aggregator (eval-aggregator temp-name expression)
        aggregators (conj aggregators-so-far new-aggregator)]
    [aggregators {:type "fieldAccess" :fieldName temp-name}]))

(declare eval-post-aggregator)

(defn- build-arithmetic-postagg [name operator args aggregators-so-far]
  (let [accumulator (fn [[aggregators fields] arg]
                      (let [[new-aggregators post-aggregator] (eval-post-aggregator nil arg aggregators)]
                        [new-aggregators (conj fields post-aggregator)]))
        [aggregators fields] (reduce accumulator [aggregators-so-far []] args)]
    [aggregators (cond-> {:type "arithmetic" :fn (str operator) :fields fields}
                         name (assoc :name name))]))

(defn eval-post-aggregator [name expression aggregators-so-far]
  (match expression
    ([(op :guard arithmetic-operator?) & args] :seq) (build-arithmetic-postagg name op args aggregators-so-far)
    (value :guard number?) (build-constant-postagg value aggregators-so-far)
    ([(_ :guard aggregation?) & _] :seq) (build-field-access-postagg expression aggregators-so-far)))

(defn eval-expression
  "First we need to discriminate between and expression that should return only an aggregator and one that should return a post-aggregator besides its generated aggregators"
  [name expression]
  (match expression
    ([(_ :guard aggregation?) & _] :seq) [[(eval-aggregator name expression)] nil]
    ([(_ :guard arithmetic-operator?) & _] :seq) (eval-post-aggregator name expression [])))

(defn measure->druid [{:keys [name expression]}]
  (let [[aggregators post-aggregator] (eval-expression name (read-string expression))]
    {:aggregations aggregators :postAggregations (remove nil? [post-aggregator])}))
