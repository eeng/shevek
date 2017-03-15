(ns pivot.dw-test
  (:require-macros [cljs.test :refer [deftest testing is]])
  (:require [pivot.dw :as dw]
            [pivot.lib.dates :refer [parse-time now]]))

(deftest to-interval-test []
  (testing "period :latest-day"
    (is (= [(parse-time "2017-03-05T17:28") (parse-time "2017-03-06T17:28")]
           (dw/to-interval {:selected-period :latest-day :max-time (parse-time "2017-03-06T17:28:00")})))
    (is (= [(parse-time "2017-03-05T17:28:01") (parse-time "2017-03-06T17:28:01")]
           (dw/to-interval {:selected-period :latest-day :max-time (parse-time "2017-03-06T17:28:00.563Z")}))))

  (testing "period :latest-month"
    (is (= [(parse-time "2017-02-06T17:28") (parse-time "2017-03-06T17:28")]
           (dw/to-interval {:selected-period :latest-month :max-time (parse-time "2017-03-06T17:28")}))))

  (testing "period :latest-week"
    (is (= [(parse-time "2017-02-27T17:28") (parse-time "2017-03-06T17:28")]
           (dw/to-interval {:selected-period :latest-week :max-time (parse-time "2017-03-06T17:28")}))))

  (testing "period :current-day"
    (with-redefs [now (constantly (parse-time "2017-03-15T18:11"))]
      (is (= [(parse-time "2017-03-15T00:00") (parse-time "2017-03-15T23:59:59.999Z")]
             (dw/to-interval {:selected-period :current-day :max-time (parse-time "2017-03-06T17:28")})))))

  (testing "period :current-month"
    (with-redefs [now (constantly (parse-time "2017-03-15T18:11"))]
      (is (= [(parse-time "2017-03-01T00:00") (parse-time "2017-03-31T23:59:59.999Z")]
             (dw/to-interval {:selected-period :current-month :max-time (parse-time "2017-03-06T17:28")}))))))
