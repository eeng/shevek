(ns shevek.schema.manager-test
  (:require [clojure.test :refer :all]
            [shevek.test-helper :refer [it]]
            [shevek.makers :refer [make!]]
            [shevek.asserts :refer [submaps?]]
            [shevek.schema.manager :refer [discover! update-cubes calculate-expression]]
            [shevek.schema.metadata :refer [cubes dimensions-and-measures]]
            [shevek.schema.repository :refer [find-cubes]]
            [shevek.schemas.cube :refer [Cube]]
            [shevek.db :refer [db]]
            [com.rpl.specter :refer [select ALL]]))

(deftest discover-tests
  (it "initial descovery should save all cubes with their dimensions and metrics"
    (with-redefs [cubes (constantly ["wikiticker" "vtol_stats"])
                  dimensions-and-measures
                  (fn [_ cube]
                    (case cube
                      "wikiticker" [[{:name "region" :type "STRING"}]
                                    [{:name "added" :type "longSum"}]]
                      "vtol_stats" [[{:name "path" :type "LONG"}]
                                    [{:name "requests" :type "hyperUnique"}]]))]
      (let [cubes (discover! nil db)]
        (is (submaps? [{:name "wikiticker"} {:name "vtol_stats"}] cubes))
        (is (submaps? [{:name "region" :type "STRING"}
                       {:name "path" :type "LONG"}]
                      (mapcat :dimensions cubes)))
        (is (submaps? [{:name "added" :expression "(sum $added)"}
                       {:name "requests" :expression "(count-distinct $requests)"}]
                      (mapcat :measures cubes)))
        (is (= 2 (->> cubes (map :_id) (filter identity) count)))))))

(deftest update-cubes-tests
  (testing "discovery use cases"
    (it "discovery of a new cube"
      (let [c1 (make! Cube {:name "c1"
                            :dimensions [{:name "d1" :type "STRING"}]
                            :measures [{:name "m1" :type "count"}]})]
        (update-cubes db [(dissoc c1 :_id)
                          {:name "c2"
                           :dimensions [{:name "d2" :type "STRING"}]
                           :measures [{:name "m2" :type "count"}]}])
        (let [cubes (find-cubes db)]
          (is (= ["c1" "c2"] (map :name cubes)))
          (is (= (:_id c1) (:_id (first cubes))))
          (is (= ["d1" "d2"] (select [ALL :dimensions ALL :name] cubes)))
          (is (= ["m1" "m2"] (select [ALL :measures ALL :name] cubes))))))

    (it "existing cube with a new dimension (d2), a deleted one (d1) and a changed measure type"
      (let [c1 (make! Cube {:name "c1" :title "C1"
                            :dimensions [{:name "d1" :type "STRING" :title "D1"}]
                            :measures [{:name "m1" :type "count" :title "M1"}]})]
        (update-cubes db [{:name "c1"
                           :dimensions [{:name "d2" :type "STRING"}]
                           :measures [{:name "m1" :type "longSum"}]}])
        (let [cubes (find-cubes db)]
          (is (= [["c1" "C1"]] (map (juxt :name :title) cubes)))
          (is (= (:_id c1) (:_id (first cubes))))
          (is (= [["d1" "D1"] ["d2" "D2"]] (map (juxt :name :title) (select [ALL :dimensions ALL] cubes))))
          (is (= [["m1" "M1" "longSum"]] (map (juxt :name :title :type) (select [ALL :measures ALL] cubes))))))))

  (testing "seed use cases"
    (it "updating cube title"
      (make! Cube {:name "stats"})
      (update-cubes db [{:name "stats" :title "Statistics"}])
      (is (= "Statistics" (-> (find-cubes db) first :title)))
      (update-cubes db [{:name "stats" :title "Statistics 2"}])
      (is (= "Statistics 2" (-> (find-cubes db) first :title))))

    (it "changing the default measure expression and format"
      (make! Cube {:name "sales" :measures [{:name "amount" :expression "(sum $amount)"}]})
      (update-cubes db [{:name "sales" :measures [{:name "amount" :expression "(/ (sum $amount) 100)" :format "$0.00"}]}])
      (is (submaps? [{:expression "(/ (sum $amount) 100)" :format "$0.00"}] (-> (find-cubes db) first :measures))))

    (it "adding new derived measure"
      (make! Cube {:name "sales" :measures []})
      (update-cubes db [{:name "sales" :measures [{:name "amount" :expression "(/ (sum $amount) 100)"}]}])
      (is (submaps? [{:name "amount" :expression "(/ (sum $amount) 100)"}] (-> (find-cubes db) first :measures))))))

(deftest calculate-expression-tests
  (are [x y] (= x (calculate-expression y))
    "(sum $amount)" {:name "amount" :type "longSum"}
    "(sum $amount)" {:name "amount" :type "doubleSum"}
    "(max $amount)" {:name "amount" :type "longMax"}
    "(count-distinct $amount)" {:name "amount" :type "hyperUnique"}))
