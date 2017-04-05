(ns shevek.schema.manager-test
  (:require [clojure.test :refer :all]
            [shevek.test-helper :refer [with-clean-db]]
            [shevek.asserts :refer [submaps?]]
            [shevek.schema.manager :refer [discover!]]
            [shevek.schema.metadata :refer [cubes dimensions-and-measures]]
            [shevek.schema.repository :refer [save-cube find-cubes]]
            [shevek.db :as db]
            [com.rpl.specter :refer [select ALL]]))

(def db (db/db))

(deftest initial-discovery-test
  (with-redefs [cubes (constantly ["wikiticker" "vtol_stats"])
                dimensions-and-measures
                (fn [_ cube]
                  (case cube
                    "wikiticker" [[{:name "region" :type "STRING"}]
                                  [{:name "added" :type "longSum"}]]
                    "vtol_stats" [[{:name "path" :type "STRING"}]
                                  [{:name "requests" :type "longSum"}]]))]
    (let [cubes (discover! nil db)]
      (is (submaps? [{:name "wikiticker"} {:name "vtol_stats"}] cubes))
      (is (submaps? [{:name "region" :type "STRING"}] (-> cubes first :dimensions)))
      (is (submaps? [{:name "added" :type "longSum"}] (-> cubes first :measures)))
      (is (= 2 (->> cubes (map :_id) (filter identity) count))))))

(deftest subsequent-discovery-with-one-new-cube-test
  (let [c1 (save-cube db {:name "c1"
                          :dimensions [{:name "d1" :type "STRING"}]
                          :measures [{:name "m1" :type "count"}]})]
    (with-redefs
      [cubes (constantly ["c1" "c2"])
       dimensions-and-measures (fn [_ cube]
                                 (case cube
                                   "c1" ((juxt :dimensions :measures) c1)
                                   "c2" [[{:name "d2" :type "STRING"}] [{:name "m2" :type "count"}]]))]
      (discover! nil db)
      (let [cubes (find-cubes db)]
        (is (= ["c1" "c2"] (map :name cubes)))
        (is (= ["d1" "d2"] (select [ALL :dimensions ALL :name] cubes)))
        (is (= ["m1" "m2"] (select [ALL :measures ALL :name] cubes)))))))

(use-fixtures :each with-clean-db)
