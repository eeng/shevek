(ns shevek.engine.druid-sql.solver
  (:require [clojure.string :as str]
            [shevek.driver.druid :as driver]
            [shevek.domain.dimension :refer [time-dimension? sort-by-other-dimension?]]
            [shevek.engine.utils :refer [time-zone defaultLimit]]
            [com.rpl.specter :refer [transform ALL]]))

(defn- wrap-string [s]
  (format "'%s'" (str/escape (or s "") {\' "''"})))

(defn wrap-strings [list]
  (->> list (map wrap-string) (str/join ", ")))

(def reserved-words
  #{"COUNT" "EPOCH" "SECOND" "MINUTE" "HOUR" "DAY" "MONTH" "YEAR" "WEEK"})

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
      (= "hyperUnique" type) (format "COUNT(DISTINCT %s)" name)
      (= "rowCount" type) "COUNT(*)"
      (= "FLOAT" type) (format "CAST(%s AS FLOAT)" name)
      :else name)))

(defn- select-expr [{:keys [name expression] :as dim}]
  {:pre [(string? name)]}
  (let [expr (calculate-expression dim)]
    (if (= expr name)
      (escape-id name)
      (str expr " AS " (escape-id name)))))

(defn- filter->sql [{:keys [name interval operator value] :as filter}]
  {:pre [(string? name)]}
  (let [expr (calculate-expression filter)]
    (cond
      interval
      (let [[from to] interval]
        (format "%s BETWEEN '%s' AND '%s'" name from to))

      (= operator "is")
      (format "%s = %s" expr (wrap-string value))

      (= operator "include")
      (format "%s IN (%s)" expr (wrap-strings value))

      (= operator "exclude")
      (format "%s NOT IN (%s)" expr (wrap-strings value))

      (= operator "search")
      (format "LOWER(%s) LIKE %s" expr (wrap-string (str "%" (str/lower-case value) "%")))

      :else
      (throw (ex-info "Filter not supported" {:filter filter})))))

(defn- self-and-sort-if-is-other-dim [{:keys [sort-by] :as dim}]
  (if (sort-by-other-dimension? dim)
    [sort-by dim]
    [dim]))

(defrecord SelectClause [dimensions measures]
  Object
  (toString [_]
    (let [expressions (map select-expr (concat (mapcat self-and-sort-if-is-other-dim dimensions) measures))]
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
    (when (seq dimensions)
      (let [expressions (->> dimensions
                             (mapcat self-and-sort-if-is-other-dim)
                             (map calculate-expression))]
        (str "GROUP BY " (str/join ", " expressions))))))

(defn- sort-by->sql [{:keys [name descending]}]
  (str (escape-id name) " " (if descending "DESC" "ASC")))

(defn- add-default-sort-if-none [dimensions measures sorts-by]
  (if (or (seq sorts-by) (empty? dimensions))
    sorts-by
    [(assoc (first measures) :descending true)]))

(defrecord OrderClause [dimensions measures]
  Object
  (toString [_]
    (let [expressions (->> dimensions
                           (map :sort-by)
                           (remove nil?)
                           (add-default-sort-if-none dimensions measures)
                           (map sort-by->sql))]
      (when (seq expressions)
        (str "ORDER BY " (str/join ", " expressions))))))

(defrecord LimitClause [dimension]
  Object
  (toString [_]
    (when dimension
      (str "LIMIT " (or (:limit dimension) defaultLimit)))))

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
     :limit (LimitClause. dimension))))

(defn to-sql [query]
  (-> query to-ast to-str))

(defn resolve-expanded-query [driver query]
  (driver/send-query driver {:query (to-sql query)
                             :sqlTimeZone (time-zone query)}))

#_(resolve-expanded-query
   (driver/http-druid-driver "http://localhost:8082")
   {:cube "wikiticker"
    :measures [{:name "added" :expression "sum(added)"}
               {:name "count" :type "longSum"}]
    :dimension {:name "countryName"}
    :filters [{:name "countryName" :operator "search" :value "Arg"}]
    :limit 10})
