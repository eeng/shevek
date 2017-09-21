(ns shevek.lib.dw.time-test
  (:require-macros [cljs.test :refer [deftest testing is]])
  (:require [shevek.lib.dw.time :refer [to-interval format-period default-granularity]]
            [shevek.lib.dates :refer [now date-time]]))

(def d date-time)

(deftest to-interval-test []
  (testing "period :latest-hour"
    (is (= [(d 2017 3 6 16 28) (d 2017 3 6 17 28)]
           (to-interval :latest-hour (d 2017 3 6 17 27 30)))))

  (testing "period :latest-day"
    (is (= [(d 2017 3 5 17 29) (d 2017 3 6 17 29)]
           (to-interval :latest-day (d 2017 3 6 17 28)))))

  (testing "period :current-day"
    (with-redefs [now (constantly (d 2017 3 15 18 11))]
      (is (= [(d 2017 3 15) (d 2017 3 15 23 59 59 999)]
             (to-interval :current-day nil)))))

  (testing "period :current-week"
    (with-redefs [now (constantly (d 2017 3 20 15 10))]
      (is (= [(d 2017 3 20) (d 2017 3 26 23 59 59 999)]
             (to-interval :current-week nil))))
    (with-redefs [now (constantly (d 2017 3 22 23 59))]
      (is (= [(d 2017 3 20) (d 2017 3 26 23 59 59 999)]
             (to-interval :current-week nil))))
    (with-redefs [now (constantly (d 2017 3 26 10 59))]
      (is (= [(d 2017 3 20) (d 2017 3 26 23 59 59 999)]
             (to-interval :current-week nil))))
    (with-redefs [now (constantly (d 2017 3 27 0 1))]
      (is (= [(d 2017 3 27) (d 2017 4 2 23 59 59 999)]
             (to-interval :current-week nil)))))

  (testing "period :current-month"
    (with-redefs [now (constantly (d 2017 3 15 18 11))]
      (is (= [(d 2017 3 1) (d 2017 3 31 23 59 59 999)]
             (to-interval :current-month nil)))))

  (testing "period :current-quarter"
    (with-redefs [now (constantly (d 2017 3 15))]
      (is (= [(d 2017 1 1) (d 2017 3 31 23 59 59 999)]
             (to-interval :current-quarter nil))))
    (with-redefs [now (constantly (d 2017 4 3))]
      (is (= [(d 2017 4 1) (d 2017 6 30 23 59 59 999)]
             (to-interval :current-quarter nil))))
    (with-redefs [now (constantly (d 2017 12 31))]
      (is (= [(d 2017 10 1) (d 2017 12 31 23 59 59 999)]
             (to-interval :current-quarter nil))))
    (with-redefs [now (constantly (d 2018 1 2))]
      (is (= [(d 2018 1 1) (d 2018 3 31 23 59 59 999)]
             (to-interval :current-quarter nil)))))

  (testing "period :current-year"
    (with-redefs [now (constantly (d 2017 3 15))]
      (is (= [(d 2017 1 1) (d 2017 12 31 23 59 59 999)]
             (to-interval :current-year nil))))
    (with-redefs [now (constantly (d 2018 6 2))]
      (is (= [(d 2018 1 1) (d 2018 12 31 23 59 59 999)]
             (to-interval :current-year nil)))))

  (testing "period :previous-day"
    (with-redefs [now (constantly (d 2017 3 15 18 11))]
      (is (= [(d 2017 3 14) (d 2017 3 14 23 59 59 999)]
             (to-interval :previous-day nil)))))

  (testing "period :previous-week"
    (with-redefs [now (constantly (d 2017 3 20 15 10))]
      (is (= [(d 2017 3 13) (d 2017 3 19 23 59 59 999)]
             (to-interval :previous-week nil))))
    (with-redefs [now (constantly (d 2017 3 24 8 10))]
      (is (= [(d 2017 3 13) (d 2017 3 19 23 59 59 999)]
             (to-interval :previous-week nil)))))

  (testing "period :previous-month"
    (with-redefs [now (constantly (d 2017 3 15 18 11))]
      (is (= [(d 2017 2 1) (d 2017 2 28 23 59 59 999)]
             (to-interval :previous-month nil))))
    (with-redefs [now (constantly (d 2017 1 15 18 11))]
      (is (= [(d 2016 12 1) (d 2016 12 31 23 59 59 999)]
             (to-interval :previous-month nil)))))

  (testing "period :previous-quarter"
    (with-redefs [now (constantly (d 2017 3 15))]
      (is (= [(d 2016 10 1) (d 2016 12 31 23 59 59 999)]
             (to-interval :previous-quarter nil))))
    (with-redefs [now (constantly (d 2017 4 3))]
      (is (= [(d 2017 1 1) (d 2017 3 31 23 59 59 999)]
             (to-interval :previous-quarter nil)))))

  (testing "period :previous-year"
    (with-redefs [now (constantly (d 2017 3 15))]
      (is (= [(d 2016 1 1) (d 2016 12 31 23 59 59 999)]
             (to-interval :previous-year nil))))))

(deftest format-period-test
  (testing "periods :latest-xx"
    (is (= "Apr 24, 4:42pm - Apr 24, 5:42pm" (format-period :latest-hour (d 2000 4 24 17 41 59))))
    (is (= "Apr 23, 5:42pm - Apr 24, 5:42pm" (format-period :latest-day (d 2000 4 24 17 41 59)))))

  (testing "periods :current-xx"
    (with-redefs [now (constantly (d 2016 4 3 17 30))]
      (is (= "Apr 3, 2016" (format-period :current-day nil)))
      (is (= "Mar 28, 2016 - Apr 3, 2016" (format-period :current-week nil))))))

(deftest default-granularity-test
  (letfn [(time-dim
           ([period] {:name "__time" :period period})
           ([from to] {:name "__time" :interval [from to]}))]

    (testing "when the period span less than a week it should be PT1H"
      (is (= "PT1H" (default-granularity {:filters [(time-dim :latest-day)]})))
      (is (= "PT1H" (default-granularity {:filters [(time-dim (d 2016 1 1) (d 2016 1 2))]})))
      (is (= "PT1H" (default-granularity {:filters [(time-dim (d 2016 1 1) (d 2016 1 8))]}))))

    (testing "when the period span between than a week and few months it should be P1D"
      (is (= "P1D" (default-granularity {:filters [(time-dim :current-month)]})))
      (is (= "P1D" (default-granularity {:filters [(time-dim (d 2016 1 1) (d 2016 1 9))]})))
      (is (= "P1D" (default-granularity {:filters [(time-dim (d 2016 1 1) (d 2016 2 28))]}))))

    (testing "when the period span more than a few months it should be P1M"
      (is (= "P1M" (default-granularity {:filters [(time-dim :current-year)]})))
      (is (= "P1M" (default-granularity {:filters [(time-dim (d 2016 1 1) (d 2016 4 1))]}))))))
