(ns shevek.dw.engine-test
  (:require [clojure.test :refer :all]
            [shevek.dw.druid-driver :refer [DruidDriver]]
            [shevek.dw.engine :as e]
            [clj-fakes.core :as f]))

; TODO quizas con un with-redefs no hace falta este protocol ni la lib clj-fakes, ver el schema_test. Quizas no se pueda hacer el arg matcher que hago con la lib pero es lo de menos
(deftest engine-test
  (testing "cubes should return a vector of datasources"
    (let [fake-dw (reify DruidDriver
                    (datasources [_] ["wikiticker" "another"]))]
      (is (= ["wikiticker" "another"] (e/cubes fake-dw)))))

  (testing "dimensions-and-measures should return a pair of vectors, first the dimensions and second the measures"
    (f/with-fakes
      (let [dw (f/reify-fake DruidDriver
                (send-query :fake
                            [[(f/arg #(and (= (:queryType %) "segmentMetadata")
                                           (= (-> % :dataSource :name) "wikiticker")))]
                             [{:columns {:region {:type "STRING"}
                                         :city {:type "STRING"}
                                         :added {:type "LONG"}}
                               :aggregators {:added {:type "longSum"}}}]]))]
        (is (= [[{:name "region" :type "STRING"} {:name "city" :type "STRING"}]
                [{:name "added" :type "longSum"}]]
               (e/dimensions-and-measures dw "wikiticker")))))))
