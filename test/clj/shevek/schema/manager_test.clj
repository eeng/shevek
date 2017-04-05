(ns shevek.schema.manager-test
  (:require [clojure.test :refer :all]
            [shevek.test-helper :refer [with-clean-db]]
            [shevek.asserts :refer [submaps?]]
            [shevek.schema.manager :refer [discover!]]
            [shevek.schema.metadata :refer [cubes dimensions-and-measures]]
            [shevek.db :refer [db]]))

(deftest discover!-test
  (testing "initial discovery should insert cubes and theirs dimensions/measures"
    (with-redefs [cubes (constantly ["wikiticker" "vtol_stats"])
                  dimensions-and-measures
                  (fn [_ cube]
                    (case cube
                      "wikiticker" [[{:name "region" :type "STRING"}]
                                    [{:name "added" :type "longSum"}]]
                      "vtol_stats" [[{:name "path" :type "STRING"}]
                                    [{:name "requests" :type "longSum"}]]))]
      (let [cubes (discover! nil (db))]
        (is (submaps? [{:name "wikiticker"} {:name "vtol_stats"}] cubes))
        (is (submaps? [{:name "region" :type "STRING"}] (-> cubes first :dimensions)))
        (is (submaps? [{:name "added" :type "longSum"}] (-> cubes first :measures)))
        (is (= 2 (->> cubes (map :_id) (filter identity) count)))))))

(use-fixtures :each with-clean-db)
