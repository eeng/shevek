(ns shevek.dw-test
  (:require-macros [cljs.test :refer [deftest testing is]])
  (:require [shevek.dw :refer [to-interval format-period default-granularity]]
            [shevek.lib.dates :refer [parse-time now]]))

(deftest to-interval-test []
  (testing "period :latest-hour"
    (is (= [(parse-time "2017-03-06T16:27:30") (parse-time "2017-03-06T17:27:30")]
           (to-interval :latest-hour (parse-time "2017-03-06T17:27:30")))))

  (testing "period :latest-day"
    (is (= [(parse-time "2017-03-05T17:28") (parse-time "2017-03-06T17:28")]
           (to-interval :latest-day (parse-time "2017-03-06T17:28")))))

  (testing "period :current-day"
    (with-redefs [now (constantly (parse-time "2017-03-15T18:11"))]
      (is (= [(parse-time "2017-03-15T00:00") (parse-time "2017-03-15T23:59:59.999Z")]
             (to-interval :current-day nil)))))

  (testing "period :current-week"
    (with-redefs [now (constantly (parse-time "2017-03-20T15:10"))]
      (is (= [(parse-time "2017-03-20T00:00") (parse-time "2017-03-26T23:59:59.999Z")]
             (to-interval :current-week nil))))
    (with-redefs [now (constantly (parse-time "2017-03-22T23:59"))]
      (is (= [(parse-time "2017-03-20T00:00") (parse-time "2017-03-26T23:59:59.999Z")]
             (to-interval :current-week nil))))
    (with-redefs [now (constantly (parse-time "2017-03-26T10:59"))]
      (is (= [(parse-time "2017-03-20T00:00") (parse-time "2017-03-26T23:59:59.999Z")]
             (to-interval :current-week nil))))
    (with-redefs [now (constantly (parse-time "2017-03-27T00:01"))]
      (is (= [(parse-time "2017-03-27T00:00") (parse-time "2017-04-02T23:59:59.999Z")]
             (to-interval :current-week nil)))))

  (testing "period :current-month"
    (with-redefs [now (constantly (parse-time "2017-03-15T18:11"))]
      (is (= [(parse-time "2017-03-01T00:00") (parse-time "2017-03-31T23:59:59.999Z")]
             (to-interval :current-month nil)))))

  (testing "period :current-quarter"
    (with-redefs [now (constantly (parse-time "2017-03-15"))]
      (is (= [(parse-time "2017-01-01T00:00") (parse-time "2017-03-31T23:59:59.999Z")]
             (to-interval :current-quarter nil))))
    (with-redefs [now (constantly (parse-time "2017-04-03"))]
      (is (= [(parse-time "2017-04-01T00:00") (parse-time "2017-06-30T23:59:59.999Z")]
             (to-interval :current-quarter nil))))
    (with-redefs [now (constantly (parse-time "2017-12-31"))]
      (is (= [(parse-time "2017-10-01T00:00") (parse-time "2017-12-31T23:59:59.999Z")]
             (to-interval :current-quarter nil))))
    (with-redefs [now (constantly (parse-time "2018-01-02"))]
      (is (= [(parse-time "2018-01-01T00:00") (parse-time "2018-03-31T23:59:59.999Z")]
             (to-interval :current-quarter nil)))))

  (testing "period :current-year"
    (with-redefs [now (constantly (parse-time "2017-03-15"))]
      (is (= [(parse-time "2017-01-01T00:00") (parse-time "2017-12-31T23:59:59.999Z")]
             (to-interval :current-year nil))))
    (with-redefs [now (constantly (parse-time "2018-06-02"))]
      (is (= [(parse-time "2018-01-01T00:00") (parse-time "2018-12-31T23:59:59.999Z")]
             (to-interval :current-year nil)))))

  (testing "period :previous-day"
    (with-redefs [now (constantly (parse-time "2017-03-15T18:11"))]
      (is (= [(parse-time "2017-03-14T00:00") (parse-time "2017-03-14T23:59:59.999Z")]
             (to-interval :previous-day nil)))))

  (testing "period :previous-week"
    (with-redefs [now (constantly (parse-time "2017-03-20T15:10"))]
      (is (= [(parse-time "2017-03-13T00:00") (parse-time "2017-03-19T23:59:59.999Z")]
             (to-interval :previous-week nil))))
    (with-redefs [now (constantly (parse-time "2017-03-24T08:10"))]
      (is (= [(parse-time "2017-03-13T00:00") (parse-time "2017-03-19T23:59:59.999Z")]
             (to-interval :previous-week nil)))))

  (testing "period :previous-month"
    (with-redefs [now (constantly (parse-time "2017-03-15T18:11"))]
      (is (= [(parse-time "2017-02-01T00:00") (parse-time "2017-02-28T23:59:59.999Z")]
             (to-interval :previous-month nil))))
    (with-redefs [now (constantly (parse-time "2017-01-15T18:11"))]
      (is (= [(parse-time "2016-12-01T00:00") (parse-time "2016-12-31T23:59:59.999Z")]
             (to-interval :previous-month nil)))))

  (testing "period :previous-quarter"
    (with-redefs [now (constantly (parse-time "2017-03-15"))]
      (is (= [(parse-time "2016-10-01T00:00") (parse-time "2016-12-31T23:59:59.999Z")]
             (to-interval :previous-quarter nil))))
    (with-redefs [now (constantly (parse-time "2017-04-03"))]
      (is (= [(parse-time "2017-01-01T00:00") (parse-time "2017-03-31T23:59:59.999Z")]
             (to-interval :previous-quarter nil)))))

  (testing "period :previous-year"
    (with-redefs [now (constantly (parse-time "2017-03-15"))]
      (is (= [(parse-time "2016-01-01T00:00") (parse-time "2016-12-31T23:59:59.999Z")]
             (to-interval :previous-year nil))))))

(deftest format-period-test
  (testing "periods :latest-xx"
    (is (= "Apr 24, 4:42pm - Apr 24, 5:42pm" (format-period :latest-hour (parse-time "2000-04-24T17:42"))))
    (is (= "Apr 23, 5:42pm - Apr 24, 5:42pm" (format-period :latest-day (parse-time "2000-04-24T17:42")))))

  (testing "periods :current-xx"
    (with-redefs [now (constantly (parse-time "2016-04-03T17:30"))]
      (is (= "Apr 3, 2016" (format-period :current-day nil)))
      (is (= "Mar 28, 2016 - Apr 3, 2016" (format-period :current-week nil))))))

(deftest default-granularity-test
  (letfn [(time-dim
           ([period] {:name "__time" :period period})
           ([from to] {:name "__time" :interval [(parse-time from) (parse-time to)]}))]

    (testing "when the period span less than a week it should be PT1H"
      (is (= "PT1H" (default-granularity {:filter [(time-dim :latest-day)]})))
      (is (= "PT1H" (default-granularity {:filter [(time-dim "2016-01-01" "2016-01-02")]})))
      (is (= "PT1H" (default-granularity {:filter [(time-dim "2016-01-01" "2016-01-08")]}))))

    (testing "when the period span between than a week and few months it should be P1D"
      (is (= "P1D" (default-granularity {:filter [(time-dim :current-month)]})))
      (is (= "P1D" (default-granularity {:filter [(time-dim "2016-01-01" "2016-01-09")]})))
      (is (= "P1D" (default-granularity {:filter [(time-dim "2016-01-01" "2016-02-28")]}))))

    (testing "when the period span more than a few months it should be P1M"
      (is (= "P1M" (default-granularity {:filter [(time-dim :current-year)]})))
      (is (= "P1M" (default-granularity {:filter [(time-dim "2016-01-01" "2016-04-01")]}))))))
