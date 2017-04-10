(ns shevek.reports.repository-test
  (:require [clojure.test :refer :all]
            [shevek.makers :refer [make]]
            [shevek.asserts :refer [submap?]]
            [shevek.schema.schemas :refer [Cube Measure]]
            [shevek.querying.schemas :refer [Viewer]]
            [shevek.reports.repository :refer [viewer->report]])
  (:import [org.bson.types ObjectId]))

(deftest viewer->report-tests
  (testing "transform a cube to his cube-id"
    (let [cube-id (ObjectId.)]
      (is (= {:cube-id cube-id}
             (-> {:cube (make Cube {:_id cube-id})}
                 viewer->report
                 (select-keys [:cube-id :cube]))))))

  (testing "should store only the selected measures names"
    (is (= ["added" "deleted"]
           (->> {:measures [(make Measure {:name "added"})
                            (make Measure {:name "deleted"})]}
                (make Viewer) viewer->report :measures)))))
