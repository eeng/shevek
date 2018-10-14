(ns shevek.engine.druid-sql.query
  (:require [clojure.string :as str]
            [shevek.engine.druid.driver :as driver]))

(defn- wrap-string [s]
  (format "'%s'" (str/escape s {\' "''"})))

(defn wrap-strings [list]
  (->> list (map wrap-string) (str/join ", ")))

(defn- dimension->sql [{:keys [name expression type]}]
  (cond
    expression (str expression " AS " name)
    (nil? type) name
    (re-matches #".*Sum" type) (format "SUM(%s) AS %s" name name)
    (re-matches #".*Max" type) (format "MAX(%s) AS %s" name name)
    (re-matches #".*Min" type) (format "Min(%s) AS %s" name name)
    (re-matches #"hyperUnique" type) (format "COUNT(DISTINCT %s) AS %s" name name)
    :else name))

(defn- filters->sql [{:keys [name interval operator value] :as filter}]
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

(defn- to-str [{:keys [select from where group limit]}]
  (->>
   [(str "SELECT " (str/join ", " select))
    (str "FROM " from)
    (when (seq where)
      (str "WHERE " (str/join " AND " where)))
    (when (seq group)
      (str "GROUP BY " (str/join ", " group)))
    (when limit
      (str "LIMIT " limit))]
   (remove nil?)
   (str/join " ")))

(defn- to-ast [{:keys [cube dimension measures filters limit] :or {limit 100}}]
  (let [dimensions (remove nil? [dimension])]
    {:select (map dimension->sql (concat dimensions measures))
     :from cube
     :where (map filters->sql filters)
     :group (range 1 (inc (count dimensions)))
     :limit (when dimension limit)}))

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
