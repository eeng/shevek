(ns shevek.engine.druid-sql.query
  (:require [clojure.string :as str]
            [shevek.engine.druid.driver :as driver]
            [shevek.domain.dimension :refer [time-dimension?]]))

(defn- wrap-string [s]
  (format "'%s'" (str/escape s {\' "''"})))

(defn wrap-strings [list]
  (->> list (map wrap-string) (str/join ", ")))

(defn- dimension->sql [{:keys [name expression granularity] :as dim}]
  (cond
    expression (str expression " AS " name)
    (time-dimension? dim) (format "TIME_FLOOR(%s, %s) AS %s" name (wrap-string granularity) name)
    :else name))

(defn- measure->sql [{:keys [name expression type]}]
  (cond
    expression (str expression " AS " name)
    (nil? type) name
    (re-matches #".*Sum" type) (format "SUM(%s) AS %s" name name)
    (re-matches #".*Max" type) (format "MAX(%s) AS %s" name name)
    (re-matches #".*Min" type) (format "Min(%s) AS %s" name name)
    (re-matches #"hyperUnique" type) (format "COUNT(DISTINCT %s) AS %s" name name)
    :else name))

(defn- filter->sql [{:keys [name interval operator value] :as filter}]
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

(defrecord SelectClause [expressions]
  Object
  (toString [_] (str "SELECT " (str/join ", " expressions))))

(defrecord FromClause [table]
  Object
  (toString [_] (str "FROM " table)))

(defrecord WhereClause [condition]
  Object
  (toString [_] (when-not (empty? (str condition))
                  (str "WHERE " condition))))

(defrecord AndCondition [conditions]
  Object
  (toString [_] (str/join " AND " conditions)))

(defrecord GroupClause [expressions]
  Object
  (toString [_] (str "GROUP BY " (str/join ", " expressions))))

(defrecord LimitClause [limit]
  Object
  (toString [_] (str "LIMIT " limit)))

(defn- to-str [query]
  (->> (vals query)
       (map str)
       (remove empty?)
       (str/join " ")))

(defn to-ast [{:keys [cube dimension measures filters limit] :or {limit 100}}]
  (let [dimensions (remove nil? [dimension])]
    (array-map
     :select (SelectClause. (concat (map dimension->sql dimensions) (map measure->sql measures)))
     :from (FromClause. cube)
     :where (WhereClause. (AndCondition. (map filter->sql filters)))
     :group (GroupClause. (range 1 (inc (count dimensions))))
     :limit (when dimension (LimitClause. limit)))))

(defn to-sql [query]
  (-> query to-ast to-str))

(defn- send-query [driver sql]
  (driver/send-query driver {:query sql}))

(defn exec-query [driver query]
  (->> (to-sql query)
       (send-query driver)))

#_(exec-query
   (driver/http-druid-driver "http://localhost:8082")
   {:cube "wikiticker"
    :measures [{:name "added" :expression "sum(added)"}]
    :dimension {:name "countryName"}
    :filters [{:name "countryName" :operator "search" :value "Arg"}]
    :limit 10})
