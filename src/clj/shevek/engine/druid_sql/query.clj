(ns shevek.engine.druid-sql.query
  (:require [clojure.string :as str]
            [shevek.driver.druid :as driver]
            [shevek.domain.dimension :refer [time-dimension?]]
            [com.rpl.specter :refer [transform ALL]]))

(defn- wrap-string [s]
  (format "'%s'" (str/escape (or s "") {\' "''"})))

(defn wrap-strings [list]
  (->> list (map wrap-string) (str/join ", ")))

(def reserved-words
  #{"COUNT" "DAY" "MONTH" "YEAR" "WEEK"})

(defn- needs-escaping? [s]
  (contains? reserved-words (str/upper-case s)))

(defn escape-id [s]
  (if (needs-escaping? s)
    (format "\"%s\"" (str/escape s {\" "\"\""}))
    s))

(defn- calculate-expression [{:keys [expression name granularity type] :as dim}]
  (let [name (escape-id name)]
    (cond
      expression expression
      (time-dimension? dim) (format "TIME_FLOOR(%s, %s)" name (wrap-string granularity))
      (nil? type) name
      (re-matches #".*Sum" type) (format "SUM(%s)" name)
      (re-matches #".*Max" type) (format "MAX(%s)" name)
      (re-matches #".*Min" type) (format "Min(%s)" name)
      (re-matches #"hyperUnique" type) (format "COUNT(DISTINCT %s)" name)
      (re-matches #"rowCount" type) "COUNT(*)"
      :else name)))

(defn- select-expr [{:keys [name expression] :as dim}]
  {:pre [(string? name)]}
  (let [expr (calculate-expression dim)]
    (if (= expr name)
      (escape-id name)
      (str expr " AS " (escape-id name)))))

(defn- filter->sql [{:keys [name interval operator value] :as filter}]
  {:pre [(string? name)]}
  (cond
    interval
    (let [[from to] interval]
      (format "%s BETWEEN '%s' AND '%s'" name from to))

    (= operator "is")
    (format "%s = %s" name (wrap-string value))

    (= operator "include")
    (format "%s IN (%s)" name (wrap-strings value))

    (= operator "exclude")
    (format "%s NOT IN (%s)" name (wrap-strings value))

    (= operator "search")
    (format "LOWER(%s) LIKE %s" name (wrap-string (str "%" (str/lower-case value) "%")))

    :else
    (throw (ex-info "Filter not supported" {:filter filter}))))

(defrecord SelectClause [dimensions measures]
  Object
  (toString [_]
    (let [expressions (map select-expr (concat dimensions measures))]
      (str "SELECT " (str/join ", " expressions)))))

(defrecord FromClause [table]
  Object
  (toString [_] (str "FROM " table)))

(defrecord WhereClause [condition]
  Object
  (toString [_]
    (when-not (empty? (str condition))
      (str "WHERE " condition))))

(defrecord AndCondition [filters]
  Object
  (toString [_]
    (let [conditions (map filter->sql filters)]
      (str/join " AND " conditions))))

(defrecord GroupClause [dimensions]
  Object
  (toString [_]
    (let [expressions (range 1 (inc (count dimensions)))]
      (when (seq expressions)
        (str "GROUP BY " (str/join ", " expressions))))))

(defn- sort-by->sql [{:keys [name descending]}]
  (str (escape-id name) " " (if descending "DESC" "ASC")))

(defn- add-default-sort-if-none [measures sorts-by]
  (if (seq sorts-by)
    sorts-by
    [(assoc (first measures) :descending true)]))

(defrecord OrderClause [dimensions measures]
  Object
  (toString [_]
    (let [expressions (->> dimensions
                           (map :sort-by)
                           (remove nil?)
                           (add-default-sort-if-none measures)
                           (map sort-by->sql))]
      (when (seq expressions)
        (str "ORDER BY " (str/join ", " expressions))))))

(defrecord LimitClause [limit]
  Object
  (toString [_] (str "LIMIT " (or limit 100))))

(defn- to-str [query]
  (->> (vals query)
       (map str)
       (remove empty?)
       (str/join " ")))

(defn to-ast [{:keys [cube dimension measures filters]}]
  (let [dimensions (remove nil? [dimension])
        filters (transform [ALL time-dimension?] #(assoc % :name "__time") filters)]
    (array-map
     :select (SelectClause. dimensions measures)
     :from (FromClause. cube)
     :where (WhereClause. (AndCondition. filters))
     :group (GroupClause. dimensions)
     :order (OrderClause. dimensions measures)
     :limit (when dimension (LimitClause. (:limit dimension))))))

(defn to-sql [query]
  (-> query to-ast to-str))

(defn- send-query [driver sql]
  (driver/send-query driver {:query sql}))

(defn execute-query [driver query]
  (->> (to-sql query)
       (send-query driver)))

#_(execute-query
   (driver/http-druid-driver "http://localhost:8082")
   {:cube "wikiticker"
    :measures [{:name "added" :expression "sum(added)"}
               {:name "count" :type "longSum"}]
    :dimension {:name "countryName"}
    :filters [{:name "countryName" :operator "search" :value "Arg"}]
    :limit 10})
