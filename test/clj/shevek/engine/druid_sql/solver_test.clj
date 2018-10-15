(ns shevek.engine.druid-sql.solver-test
  (:require [clojure.test :refer :all]
            [clojure.string :as str]
            [shevek.engine.druid-sql.solver :refer [to-sql to-ast]]))

(defn- sql [& args]
  (str/join " " args))

(def select-clause (comp str :select to-ast))
(def where-clause (comp str :where to-ast))
(def group-clause (comp str :group to-ast))
(def order-clause (comp str :order to-ast))
(def limit-clause (comp str :limit to-ast))

(deftest to-sql-test
  (testing "general structure"
    (is (= (sql "SELECT country, sum(added) AS added"
                "FROM wikiticker"
                "WHERE __time BETWEEN '2015' AND '2019'"
                "GROUP BY country"
                "ORDER BY added DESC"
                "LIMIT 100")
           (to-sql
            {:cube "wikiticker"
             :measures [{:name "added" :expression "sum(added)"}]
             :dimension {:name "country"}
             :filters [{:name "__time" :interval ["2015" "2019"]}]}))))

  (testing "measures"
    (testing "if an expression is indicated should be used"
      (is (= "SELECT sum(amount) AS sales"
             (select-clause {:measures [{:name "sales" :expression "sum(amount)"}]}))))

    (testing "is no expression is specified, should create one based on the type"
      (is (= "SELECT SUM(amount) AS amount"
             (select-clause {:measures [{:name "amount" :type "longSum"}]})))
      (is (= "SELECT SUM(amount) AS amount"
             (select-clause {:measures [{:name "amount" :type "doubleSum"}]})))
      (is (= "SELECT MAX(amount) AS amount"
             (select-clause {:measures [{:name "amount" :type "doubleMax"}]})))
      (is (= "SELECT COUNT(DISTINCT amount) AS amount"
             (select-clause {:measures [{:name "amount" :type "hyperUnique"}]})))
      (is (= "SELECT COUNT(*) AS rowCount"
             (select-clause {:measures [{:name "rowCount" :type "rowCount"}]})))))

  (testing "filters"
    (testing "WHERE is not added if there is no filter"
      (is (= nil
             (where-clause {}))))

    (testing "time should add a between condition"
      (is (= "WHERE __time BETWEEN '2015' AND '2019'"
             (where-clause {:filters [{:interval ["2015" "2019"]}]}))))

    (testing "is operator"
      (is (= "WHERE country = 'Argentina'"
             (where-clause {:filters [{:name "country" :operator "is" :value "Argentina"}]}))))

    (testing "include operator"
      (is (= "WHERE country IN ('China', 'Italia')"
             (where-clause {:filters [{:name "country" :operator "include" :value ["China" "Italia"]}]})))
      (is (= "WHERE country IN ('')"
             (where-clause {:filters [{:name "country" :operator "include" :value [nil]}]})))
      (is (= "WHERE country IN ('')"
             (where-clause {:filters [{:name "country" :operator "include" :value [""]}]}))))

    (testing "exclude operator"
      (is (= "WHERE country NOT IN ('Francia')"
             (where-clause {:filters [{:name "country" :operator "exclude" :value ["Francia"]}]}))))

    (testing "search operator"
      (is (= "WHERE LOWER(country) LIKE '%arg%'"
             (where-clause {:filters [{:name "country" :operator "search" :value "Arg"}]})))))

  (testing "grouping"
    (testing "when no dimension is present should not add the clause"
      (is (= nil (group-clause {}))))

    (testing "normal dimension should add a SELECT and GROUP expression"
      (let [q {:dimension {:name "country"}}]
        (is (= "SELECT country" (select-clause q)))
        (is (= "GROUP BY country" (group-clause q)))))

    (testing "time dimension should should use the granularity"
      (let [q {:dimension {:name "__time" :granularity "P1D"}}]
        (is (= "SELECT TIME_FLOOR(__time, 'P1D') AS __time"
               (select-clause q)))
        (is (= "GROUP BY TIME_FLOOR(__time, 'P1D')"
               (group-clause q)))))

    (testing "when sorting by a different dimension, that dimension should be added to the GROUP BY and to the ORDER BY clauses"
      (let [q {:dimension {:name "country" :sort-by {:name "countryIsoCode" :expression "isoExpr"}}}]
        (is (= "SELECT isoExpr AS countryIsoCode, country" (select-clause q)))
        (is (= "GROUP BY isoExpr, country" (group-clause q)))
        (is (= "ORDER BY countryIsoCode ASC" (order-clause q)))))

    (testing "dimension can have an expression"
        (is (= "SELECT upper(...) AS country"
               (select-clause {:dimension {:name "country" :expression "upper(...)"}})))))

  (testing "sorting"
    (testing "sorting by a dimension"
      (let [q {:dimension {:name "city" :sort-by {:name "city" :descending false}}}]
        (is (= "ORDER BY city ASC" (order-clause q))))
      (let [q {:dimension {:name "city" :sort-by {:name "city" :descending true :expression "upper(city)"}}}]
        (is (= "ORDER BY city DESC" (order-clause q)))))

    (testing "sorting by a metric",
      (let [q {:dimension {:name "city" :sort-by {:name "amount" :expression "sum(amount)" :measure? true}}}]
        (is (= "ORDER BY amount ASC" (order-clause q)))))

    (testing "if no sort-by is specified, should sort by the first measure desc"
      (is (= "ORDER BY amount DESC"
             (order-clause {:dimension {:name "city"}
                            :measures [{:name "amount" :expression "sum(amount)"}]}))))

    (testing "if no dimension is specified, should not add the clause"
      (is (= nil (order-clause {:measures [{:name "amount" :expression "sum(amount)"}]})))))

  (testing "limit"
    (testing "default should only be present when a dimension is indicated"
      (is (= "LIMIT 100"
             (limit-clause {:dimension {:name "country"}})))
      (is (= nil (limit-clause {}))))

    (testing "when specified should be used"
      (is (= "LIMIT 9"
             (limit-clause {:dimension {:name "country" :limit 9}})))))

  (testing "escaping"
    (testing "should escape string values on filters"
      (is (= "WHERE page = 'Someone''s house'"
             (where-clause {:filters [{:name "page" :operator "is" :value "Someone's house"}]}))))

    (testing "should escape reserved words on aliases and calculated expressions"
      (is (= "SELECT SUM(\"count\") AS \"count\""
             (select-clause {:measures [{:name "count" :type "longSum"}]})))

      (is (= "ORDER BY \"count\" DESC"
             (order-clause {:measures [{:name "count" :type "longSum"}]
                            :dimension {:name "country"}}))))))
