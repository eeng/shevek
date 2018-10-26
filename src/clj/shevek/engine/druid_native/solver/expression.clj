(ns shevek.engine.druid-native.solver.expression
  (:require [clojure.core.match :refer [match]]
            [cuerdas.core :as str]
            [shevek.lib.collections :refer [includes?]]))

(def aggregator-types {'count "count" 'sum "doubleSum" 'count-distinct "hyperUnique" 'max "doubleMax"})

(defn- field-ref->field [field-ref]
  (-> field-ref str (subs 1)))

(defn- build-aggregator [name agg-fn & [field-ref]]
  (cond-> {:type (aggregator-types agg-fn) :name name}
    field-ref (assoc :fieldName (field-ref->field field-ref))))

(defn- condition->filter [condition]
  (if (map? condition)
    (if (> (count condition) 1)
      {:type "and" :fields (map #(condition->filter (into {} [%])) condition)}
      (condition->filter (-> condition seq flatten (conj '=))))

    (match condition
      ([(op :guard #{'and 'or}) & args] :seq)
      {:type (str op) :fields (map condition->filter args)}

      (['= field-ref val] :seq)
      {:type "selector" :dimension (field-ref->field field-ref) :value val}

      (['not= field-ref val] :seq)
      {:type "not" :field {:type "selector" :dimension (field-ref->field field-ref) :value val}})))

(defn- build-filtered-aggregator [field condition aggregator]
  {:type "filtered"
   :filter (condition->filter condition)
   :aggregator aggregator
   :name field})

(defn- simple-aggregator? [f]
  (includes? (keys aggregator-types) f))

(defn- aggregator? [f]
  (includes? (conj (keys aggregator-types) 'where) f))

(defn- eval-aggregator [name expression]
  (match expression
    ([(agg-fn :guard simple-aggregator?) & args] :seq) (apply build-aggregator name agg-fn args)
    (['where condition subexp] :seq) (build-filtered-aggregator name condition (eval-aggregator name subexp))))

(declare eval-post-aggregator)

(defn- arithmetic-operator? [op]
  (includes? ['/ '* '+ '- 'quotient] op))

(defn- greatest-least-operators? [op]
  (includes? ['greatest 'least] op))

(defn- javascript-operator? [op]
  (= op 'js))

(defn- post-aggregator? [op]
  (or (arithmetic-operator? op) (greatest-least-operators? op) (javascript-operator? op)))

(defn- build-constant-postagg [value]
  [[] {:type "constant" :value value}])

(defn- build-field-access-postagg [expression tig]
  (let [temp-name (str "_t" (tig))
        aggregator (eval-aggregator temp-name expression)
        post-aggregator-type (if (or (= (:type aggregator) "hyperUnique")
                                     (= (get-in aggregator [:aggregator :type]) "hyperUnique")) ; Filtered aggregators
                               "hyperUniqueCardinality" "fieldAccess")
        post-aggregator {:type post-aggregator-type :fieldName temp-name}]
    [[aggregator] post-aggregator]))

(defn- build-fields-postagg [name args tig base-fields]
  (let [aggregators-and-post-aggs (map #(eval-post-aggregator nil % tig) args)
        aggregators (mapcat first aggregators-and-post-aggs)
        fields (map second aggregators-and-post-aggs)
        post-aggregator (assoc base-fields :fields fields :name name)]
    [aggregators post-aggregator]))

(defn- build-javascript-postagg [name fields function tig]
  (let [aggregators-and-post-aggs (map #(eval-post-aggregator nil % tig) fields)
        aggregators (mapcat first aggregators-and-post-aggs)]
    [aggregators {:type "javascript" :name name :fieldNames (map :name aggregators) :function function}]))

(defn- eval-post-aggregator [name expression tig]
  (match expression
    ([(op :guard arithmetic-operator?) & args] :seq)
    (build-fields-postagg name args tig {:type "arithmetic" :fn (str op)})

    ([(op :guard greatest-least-operators?) & args] :seq)
    (build-fields-postagg name args tig {:type (str "double" (str/capital (str op)))})

    (value :guard number?)
    (build-constant-postagg value)

    ([(_ :guard aggregator?) & _] :seq)
    (build-field-access-postagg expression tig)

    ([(_ :guard javascript-operator?) fields function] :seq)
    (build-javascript-postagg name fields function tig)))

(defn eval-expression
  "First we need to discriminate between and expression that should return only an aggregator and one that should return a post-aggregator besides its generated aggregators"
  [name expression tig]
  (match expression
    ([(_ :guard aggregator?) & _] :seq) [[(eval-aggregator name expression)] nil]
    ([(_ :guard post-aggregator?) & _] :seq) (eval-post-aggregator name expression tig)))

(defn- calculate-default-expression [{:keys [type name]}]
  (let [agg-fn (condp re-find (str type)
                 #"Max" "max"
                 #"Unique" "count-distinct"
                 #"rowCount" "count"
                 "sum")]
    (str "(" agg-fn " $" name ")")))

(defn measure->druid [{:keys [name expression] :as dim} tig]
  (let [expression (or expression (calculate-default-expression dim))
        [aggregators post-aggregator] (eval-expression name (read-string expression) tig)]
    {:aggregations aggregators :postAggregations (remove nil? [post-aggregator])}))
