(ns shevek.dw-test
  (:require-macros [cljs.test :refer [deftest testing is]])
  (:require [shevek.dw :as dw]
            [shevek.lib.dates :refer [parse-time now]]))

(deftest to-interval-test []
  (testing "period :latest-hour"
    (is (= [(parse-time "2017-03-06T16:27:30") (parse-time "2017-03-06T17:27:30")]
           (dw/to-interval :latest-hour (parse-time "2017-03-06T17:27:30")))))

  (testing "period :latest-day"
    (is (= [(parse-time "2017-03-05T17:28") (parse-time "2017-03-06T17:28")]
           (dw/to-interval :latest-day (parse-time "2017-03-06T17:28")))))

  (testing "period :current-day"
    (with-redefs [now (constantly (parse-time "2017-03-15T18:11"))]
      (is (= [(parse-time "2017-03-15T00:00") (parse-time "2017-03-15T23:59:59.999Z")]
             (dw/to-interval :current-day nil)))))

  (testing "period :current-week"
    (with-redefs [now (constantly (parse-time "2017-03-20T15:10"))]
      (is (= [(parse-time "2017-03-20T00:00") (parse-time "2017-03-26T23:59:59.999Z")]
             (dw/to-interval :current-week nil))))
    (with-redefs [now (constantly (parse-time "2017-03-22T23:59"))]
      (is (= [(parse-time "2017-03-20T00:00") (parse-time "2017-03-26T23:59:59.999Z")]
             (dw/to-interval :current-week nil))))
    (with-redefs [now (constantly (parse-time "2017-03-26T10:59"))]
      (is (= [(parse-time "2017-03-20T00:00") (parse-time "2017-03-26T23:59:59.999Z")]
             (dw/to-interval :current-week nil))))
    (with-redefs [now (constantly (parse-time "2017-03-27T00:01"))]
      (is (= [(parse-time "2017-03-27T00:00") (parse-time "2017-04-02T23:59:59.999Z")]
             (dw/to-interval :current-week nil)))))

  (testing "period :current-month"
    (with-redefs [now (constantly (parse-time "2017-03-15T18:11"))]
      (is (= [(parse-time "2017-03-01T00:00") (parse-time "2017-03-31T23:59:59.999Z")]
             (dw/to-interval :current-month nil)))))

  (testing "period :current-quarter"
    (with-redefs [now (constantly (parse-time "2017-03-15"))]
      (is (= [(parse-time "2017-01-01T00:00") (parse-time "2017-03-31T23:59:59.999Z")]
             (dw/to-interval :current-quarter nil))))
    (with-redefs [now (constantly (parse-time "2017-04-03"))]
      (is (= [(parse-time "2017-04-01T00:00") (parse-time "2017-06-30T23:59:59.999Z")]
             (dw/to-interval :current-quarter nil))))
    (with-redefs [now (constantly (parse-time "2017-12-31"))]
      (is (= [(parse-time "2017-10-01T00:00") (parse-time "2017-12-31T23:59:59.999Z")]
             (dw/to-interval :current-quarter nil))))
    (with-redefs [now (constantly (parse-time "2018-01-02"))]
      (is (= [(parse-time "2018-01-01T00:00") (parse-time "2018-03-31T23:59:59.999Z")]
             (dw/to-interval :current-quarter nil)))))

  (testing "period :current-year"
    (with-redefs [now (constantly (parse-time "2017-03-15"))]
      (is (= [(parse-time "2017-01-01T00:00") (parse-time "2017-12-31T23:59:59.999Z")]
             (dw/to-interval :current-year nil))))
    (with-redefs [now (constantly (parse-time "2018-06-02"))]
      (is (= [(parse-time "2018-01-01T00:00") (parse-time "2018-12-31T23:59:59.999Z")]
             (dw/to-interval :current-year nil)))))

  (testing "period :previous-day"
    (with-redefs [now (constantly (parse-time "2017-03-15T18:11"))]
      (is (= [(parse-time "2017-03-14T00:00") (parse-time "2017-03-14T23:59:59.999Z")]
             (dw/to-interval :previous-day nil)))))

  (testing "period :previous-week"
    (with-redefs [now (constantly (parse-time "2017-03-20T15:10"))]
      (is (= [(parse-time "2017-03-13T00:00") (parse-time "2017-03-19T23:59:59.999Z")]
             (dw/to-interval :previous-week nil))))
    (with-redefs [now (constantly (parse-time "2017-03-24T08:10"))]
      (is (= [(parse-time "2017-03-13T00:00") (parse-time "2017-03-19T23:59:59.999Z")]
             (dw/to-interval :previous-week nil)))))

  (testing "period :previous-month"
    (with-redefs [now (constantly (parse-time "2017-03-15T18:11"))]
      (is (= [(parse-time "2017-02-01T00:00") (parse-time "2017-02-28T23:59:59.999Z")]
             (dw/to-interval :previous-month nil))))
    (with-redefs [now (constantly (parse-time "2017-01-15T18:11"))]
      (is (= [(parse-time "2016-12-01T00:00") (parse-time "2016-12-31T23:59:59.999Z")]
             (dw/to-interval :previous-month nil)))))

  (testing "period :previous-quarter"
    (with-redefs [now (constantly (parse-time "2017-03-15"))]
      (is (= [(parse-time "2016-10-01T00:00") (parse-time "2016-12-31T23:59:59.999Z")]
             (dw/to-interval :previous-quarter nil))))
    (with-redefs [now (constantly (parse-time "2017-04-03"))]
      (is (= [(parse-time "2017-01-01T00:00") (parse-time "2017-03-31T23:59:59.999Z")]
             (dw/to-interval :previous-quarter nil)))))

  (testing "period :previous-year"
    (with-redefs [now (constantly (parse-time "2017-03-15"))]
      (is (= [(parse-time "2016-01-01T00:00") (parse-time "2016-12-31T23:59:59.999Z")]
             (dw/to-interval :previous-year nil))))))
