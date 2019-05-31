(ns shevek.schema.manager-test
  (:require [clojure.test :refer [use-fixtures deftest is testing]]
            [shevek.test-helper :refer [wrap-unit-tests it]]
            [shevek.asserts :refer [submaps?]]
            [shevek.makers :refer [make!]]
            [shevek.schemas.cube :refer [Cube]]
            [shevek.schema.manager :refer [seed-schema! merge-cubes update-cubes!]]
            [shevek.engine.protocol :refer [Engine cubes cube-metadata]]
            [shevek.schema.repository :refer [find-cubes]]
            [shevek.db :refer [db]]
            [clj-fakes.core :as f]))

(use-fixtures :once wrap-unit-tests)

(deftest merge-cubes-tests
  (testing "should allow to override a cube title and set a default if not specified"
    (is (= ["C1" "Cube 2" "C3"]
           (map :title
                (merge-cubes [{:name "c1"} {:name "c2"} {:name "c3"}]
                             [{:name "c2" :title "Cube 2"} {:name "c3"}])))))

  (testing "should keep configures cubes that don't exists in the discovered"
    (is (submaps? [{:name "c" :title "Cube"}]
                  (merge-cubes []
                               [{:name "c" :title "Cube"}]))))

  (testing "should keep discovered measures not configured, and should allow to add new ones"
    (is (submaps? [{:name "disc" :type "STRING"} {:name "new" :type "LONG"}]
                  (->>
                   (merge-cubes [{:name "sales" :measures [{:name "disc" :type "STRING"}]}]
                                [{:name "sales" :measures [{:name "new" :type "LONG"}]}])
                   first :measures))))

  (testing "should keep discovered dimensions not configured, and should allow to add new ones"
    (is (submaps? [{:name "disc" :type "STRING"} {:name "new" :type "LONG"}]
                  (->>
                   (merge-cubes [{:name "sales" :dimensions [{:name "disc" :type "STRING"}]}]
                                [{:name "sales" :dimensions [{:name "new" :type "LONG"}]}])
                   first :dimensions))))

  (testing "changing a measure type"
    (is (submaps? [{:name "m" :type "LONG"}]
                  (->>
                   (merge-cubes [{:name "sales" :measures [{:name "m" :type "STRING"}]}]
                                [{:name "sales" :measures [{:name "m" :type "LONG"}]}])
                   first :measures))))

  (testing "should allow to hide discovered dimensions"
    (is (submaps? [{:name "d2"}]
                  (->>
                   (merge-cubes [{:name "sales" :dimensions [{:name "d1"} {:name "d2"}]}]
                                [{:name "sales" :dimensions [{:name "d1" :hidden true}]}])
                   first :dimensions))))

  (testing "should allow to hide discovered measures"
    (is (submaps? [{:name "m2"}]
                  (->>
                   (merge-cubes [{:name "sales" :measures [{:name "m1"} {:name "m2"}]}]
                                [{:name "sales" :measures [{:name "m1" :hidden true}]}])
                   first :measures)))
    (is (submaps? []
                  (->>
                   (merge-cubes []
                                [{:name "sales" :measures [{:name "m1" :hidden true}]}])
                   first :measures))))

  ; In the latest Druid there is no way (that I know of) to reliable determine measures, because in the ingestion task the measuresSpec is optional if no rollup is specified.
  (testing "if a configured measure exists as a discovered dimension, should keep it only as a measure"
    (let [merged (merge-cubes [{:name "c" :dimensions [{:name "m1" :type "LONG"}] :measures []}]
                              [{:name "c" :dimensions [] :measures [{:name "m1"}]}])]
      (is (= [] (-> merged first :dimensions)))
      (is (submaps? [{:name "m1" :type "LONG"}] (->> merged first :measures))))))

(deftest update-cubes!-tests
  (it "should update existing cubes"
    (make! Cube {:name "sales" :min-time "x"})
    (update-cubes! db [{:name "sales" :title "Changed"}])
    (is (submaps? [{:name "sales" :min-time "x" :title "Changed"}] (find-cubes db)))))

(deftest seed-schema!-tests
  (it "should save all cubes with their dimensions and metrics"
    (f/with-fakes
      (let [dw (f/reify-fake Engine
                 (cubes :fake [[] ["wikipedia" "vtol_stats"]])
                 (cube-metadata :fake [["wikipedia"]
                                       {:dimensions [{:name "region" :type "STRING"}]
                                        :measures [{:name "added" :type "longSum"}]}
                                       ["vtol_stats"]
                                       {:dimensions [{:name "path" :type "LONG"}]
                                        :measures [{:name "requests" :type "hyperUnique"}]}]))]
        (let [cubes (do
                      (seed-schema! db dw {:discover? true})
                      (find-cubes db))]
          (is (submaps? [{:name "wikipedia"} {:name "vtol_stats"}] cubes))
          (is (submaps? [{:name "region" :type "STRING"}
                         {:name "path" :type "LONG"}]
                        (mapcat :dimensions cubes)))
          (is (submaps? [{:name "added" :type "longSum"}
                         {:name "requests" :type "hyperUnique"}]
                        (mapcat :measures cubes)))
          (is (= 2 (->> cubes (map :id) (filter identity) count))))))))
