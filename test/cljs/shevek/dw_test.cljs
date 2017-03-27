(ns shevek.dw-test
  (:require-macros [cljs.test :refer [deftest testing is]])
  (:require [shevek.dw :as dw]
            [shevek.lib.dates :refer [parse-time now]]))

(deftest to-interval-test []
  (testing "period :latest-hour"
    (is (= [(parse-time "2017-03-06T16:27:31") (parse-time "2017-03-06T17:27:31")]
           (dw/to-interval {:selected-period :latest-hour :max-time (parse-time "2017-03-06T17:27:30.563Z")}))))

  (testing "period :latest-day"
    (is (= [(parse-time "2017-03-05T17:28") (parse-time "2017-03-06T17:28")]
           (dw/to-interval {:selected-period :latest-day :max-time (parse-time "2017-03-06T17:28:00")})))
    (is (= [(parse-time "2017-03-05T17:28:01") (parse-time "2017-03-06T17:28:01")]
           (dw/to-interval {:selected-period :latest-day :max-time (parse-time "2017-03-06T17:28:00.563Z")}))))

  (testing "period :current-day"
    (with-redefs [now (constantly (parse-time "2017-03-15T18:11"))]
      (is (= [(parse-time "2017-03-15T00:00") (parse-time "2017-03-15T23:59:59.999Z")]
             (dw/to-interval {:selected-period :current-day})))))

  (testing "period :current-week"
    (with-redefs [now (constantly (parse-time "2017-03-20T15:10"))]
      (is (= [(parse-time "2017-03-20T00:00") (parse-time "2017-03-26T23:59:59.999Z")]
             (dw/to-interval {:selected-period :current-week}))))
    (with-redefs [now (constantly (parse-time "2017-03-22T23:59"))]
      (is (= [(parse-time "2017-03-20T00:00") (parse-time "2017-03-26T23:59:59.999Z")]
             (dw/to-interval {:selected-period :current-week}))))
    (with-redefs [now (constantly (parse-time "2017-03-26T10:59"))]
      (is (= [(parse-time "2017-03-20T00:00") (parse-time "2017-03-26T23:59:59.999Z")]
             (dw/to-interval {:selected-period :current-week}))))
    (with-redefs [now (constantly (parse-time "2017-03-27T00:01"))]
      (is (= [(parse-time "2017-03-27T00:00") (parse-time "2017-04-02T23:59:59.999Z")]
             (dw/to-interval {:selected-period :current-week})))))

  (testing "period :current-month"
    (with-redefs [now (constantly (parse-time "2017-03-15T18:11"))]
      (is (= [(parse-time "2017-03-01T00:00") (parse-time "2017-03-31T23:59:59.999Z")]
             (dw/to-interval {:selected-period :current-month})))))

  (testing "period :current-quarter"
    (with-redefs [now (constantly (parse-time "2017-03-15"))]
      (is (= [(parse-time "2017-01-01T00:00") (parse-time "2017-03-31T23:59:59.999Z")]
             (dw/to-interval {:selected-period :current-quarter}))))
    (with-redefs [now (constantly (parse-time "2017-04-03"))]
      (is (= [(parse-time "2017-04-01T00:00") (parse-time "2017-06-30T23:59:59.999Z")]
             (dw/to-interval {:selected-period :current-quarter}))))
    (with-redefs [now (constantly (parse-time "2017-12-31"))]
      (is (= [(parse-time "2017-10-01T00:00") (parse-time "2017-12-31T23:59:59.999Z")]
             (dw/to-interval {:selected-period :current-quarter}))))
    (with-redefs [now (constantly (parse-time "2018-01-02"))]
      (is (= [(parse-time "2018-01-01T00:00") (parse-time "2018-03-31T23:59:59.999Z")]
             (dw/to-interval {:selected-period :current-quarter})))))

  (testing "period :current-year"
    (with-redefs [now (constantly (parse-time "2017-03-15"))]
      (is (= [(parse-time "2017-01-01T00:00") (parse-time "2017-12-31T23:59:59.999Z")]
             (dw/to-interval {:selected-period :current-year}))))
    (with-redefs [now (constantly (parse-time "2018-06-02"))]
      (is (= [(parse-time "2018-01-01T00:00") (parse-time "2018-12-31T23:59:59.999Z")]
             (dw/to-interval {:selected-period :current-year})))))

  (testing "period :previous-day"
    (with-redefs [now (constantly (parse-time "2017-03-15T18:11"))]
      (is (= [(parse-time "2017-03-14T00:00") (parse-time "2017-03-14T23:59:59.999Z")]
             (dw/to-interval {:selected-period :previous-day})))))

  (testing "period :previous-week"
    (with-redefs [now (constantly (parse-time "2017-03-20T15:10"))]
      (is (= [(parse-time "2017-03-13T00:00") (parse-time "2017-03-19T23:59:59.999Z")]
             (dw/to-interval {:selected-period :previous-week}))))
    (with-redefs [now (constantly (parse-time "2017-03-24T08:10"))]
      (is (= [(parse-time "2017-03-13T00:00") (parse-time "2017-03-19T23:59:59.999Z")]
             (dw/to-interval {:selected-period :previous-week})))))

  (testing "period :previous-month"
    (with-redefs [now (constantly (parse-time "2017-03-15T18:11"))]
      (is (= [(parse-time "2017-02-01T00:00") (parse-time "2017-02-28T23:59:59.999Z")]
             (dw/to-interval {:selected-period :previous-month}))))
    (with-redefs [now (constantly (parse-time "2017-01-15T18:11"))]
      (is (= [(parse-time "2016-12-01T00:00") (parse-time "2016-12-31T23:59:59.999Z")]
             (dw/to-interval {:selected-period :previous-month})))))

  (testing "period :previous-quarter"
    (with-redefs [now (constantly (parse-time "2017-03-15"))]
      (is (= [(parse-time "2016-10-01T00:00") (parse-time "2016-12-31T23:59:59.999Z")]
             (dw/to-interval {:selected-period :previous-quarter}))))
    (with-redefs [now (constantly (parse-time "2017-04-03"))]
      (is (= [(parse-time "2017-01-01T00:00") (parse-time "2017-03-31T23:59:59.999Z")]
             (dw/to-interval {:selected-period :previous-quarter})))))

  (testing "period :previous-year"
    (with-redefs [now (constantly (parse-time "2017-03-15"))]
      (is (= [(parse-time "2016-01-01T00:00") (parse-time "2016-12-31T23:59:59.999Z")]
             (dw/to-interval {:selected-period :previous-year}))))))
