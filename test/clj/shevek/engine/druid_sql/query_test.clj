(ns shevek.engine.druid-sql.query-test
  (:require [clojure.test :refer :all]
            [clojure.string :as str]
            [shevek.engine.druid-sql.query :refer [to-sql]]))

(defn- sql [& args]
  (str/join " " args))

(defn- select-clause [query]
  (->> query to-sql (re-find #"(SELECT.*?)($| FROM)") second))

(defn- where-clause [query]
  (->> query to-sql (re-find #"\s(WHERE.*?)($| GROUP| LIMIT)") second))

(defn- limit-clause [query]
  (->> query to-sql (re-find #"LIMIT.*$")))

(deftest to-sql-test
  (testing "general structure"
    (is (= (sql "SELECT country, sum(added) AS added"
                "FROM wikiticker"
                "WHERE __time BETWEEN '2015' AND '2019'"
                "GROUP BY 1"
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
             (select-clause {:measures [{:name "amount" :type "hyperUnique"}]})))))

  (testing "filters"
    (testing "WHERE is not added if there is no filter"
      (is (= nil
             (where-clause {}))))

    (testing "time should add a between condition"
      (is (= "WHERE __time BETWEEN '2015' AND '2019'"
             (where-clause {:filters [{:name "__time" :interval ["2015" "2019"]}]}))))

    (testing "is operator"
      (is (= "WHERE country = 'Argentina'"
             (where-clause {:filters [{:name "country" :operator "is" :value "Argentina"}]}))))

    (testing "include operator"
      (is (= "WHERE country IN ('China', 'Italia')"
             (where-clause {:filters [{:name "country" :operator "include" :value ["China" "Italia"]}]}))))

    (testing "exclude operator"
      (is (= "WHERE country NOT IN ('Francia')"
             (where-clause {:filters [{:name "country" :operator "exclude" :value ["Francia"]}]}))))

    (testing "search operator"
      (is (= "WHERE LOWER(country) LIKE '%arg%'"
             (where-clause {:filters [{:name "country" :operator "search" :value "Arg"}]}))))

    (testing "should escape string values"
      (is (= "WHERE page = 'Someone''s house'"
             (where-clause {:filters [{:name "page" :operator "is" :value "Someone's house"}]})))))

  (testing "limit"
    (testing "default should only be present when a dimension is indicated"
      (is (= "LIMIT 100"
             (limit-clause {:dimension {:name "country"}})))
      (is (= nil
             (limit-clause {}))))

    (testing "when specified should be used"
      (is (= "LIMIT 9"
             (limit-clause {:dimension {:name "country"}
                            :limit 9}))))))
