(ns shevek.schema.manager-test
  (:require [clojure.test :refer [is]]
            [shevek.test-helper :refer [spec]]
            [shevek.asserts :refer [submaps?]]
            [shevek.schema.manager :refer [discover!]]
            [shevek.schema.metadata :refer [cubes dimensions-and-measures]]
            [shevek.schema.repository :refer [save-cube find-cubes]]
            [shevek.db :refer [db]]
            [com.rpl.specter :refer [select ALL]]))

(spec "initial descovery should save all cubes with their dimensions and metrics"
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

(spec "discovery of a new cube"
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
        (is (= (:_id c1) (:_id (first cubes))))
        (is (= ["d1" "d2"] (select [ALL :dimensions ALL :name] cubes)))
        (is (= ["m1" "m2"] (select [ALL :measures ALL :name] cubes)))))))

(spec "existing cube with a new dimension (d2), a deleted one (d1) and a changed measure type"
  (let [c1 (save-cube db {:name "c1" :title "C1"
                            :dimensions [{:name "d1" :type "STRING" :title "D1"}]
                            :measures [{:name "m1" :type "count" :title "M1"}]})]
    (with-redefs
      [cubes (constantly ["c1"])
       dimensions-and-measures (fn [_ cube]
                                 "c1" [[{:name "d2" :type "STRING"}]
                                       [{:name "m1" :type "longSum"}]])]
      (discover! nil db)
      (let [cubes (find-cubes db)]
        (is (= [["c1" "C1"]] (map (juxt :name :title) cubes)))
        (is (= (:_id c1) (:_id (first cubes))))
        (is (= [["d1" "D1"] ["d2" nil]] (map (juxt :name :title) (select [ALL :dimensions ALL] cubes))))
        (is (= [["m1" "M1" "longSum"]] (map (juxt :name :title :type) (select [ALL :measures ALL] cubes))))))))
