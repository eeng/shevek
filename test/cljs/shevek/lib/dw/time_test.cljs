(ns shevek.lib.dw.time-test
  (:require-macros [cljs.test :refer [deftest testing is]])
  (:require [shevek.lib.dw.time :refer [format-period default-granularity]]
            [shevek.lib.time :refer [now date-time]]))

(def d date-time)

(deftest format-period-test
  (testing "periods latest-xx"
    (is (= "Apr 24, 4:42pm - Apr 24, 5:42pm" (format-period "latest-hour" (d 2000 4 24 17 41 59))))
    (is (= "Apr 23, 5:42pm - Apr 24, 5:42pm" (format-period "latest-day" (d 2000 4 24 17 41 59)))))

  (testing "periods current-xx"
    (with-redefs [now (constantly (d 2016 4 3 17 30))]
      (is (= "Apr 3, 2016" (format-period "current-day" nil)))
      (is (= "Mar 28, 2016 - Apr 3, 2016" (format-period "current-week" nil))))))

(deftest default-granularity-test
  (letfn [(time-dim
           ([period] {:name "__time" :period period})
           ([from to] {:name "__time" :interval [from to]}))]

    (testing "when the period span less than a week it should be PT1H"
      (is (= "PT1H" (default-granularity {:filters [(time-dim "latest-day")]})))
      (is (= "PT1H" (default-granularity {:filters [(time-dim (d 2016 1 1) (d 2016 1 2))]})))
      (is (= "PT1H" (default-granularity {:filters [(time-dim (d 2016 1 1) (d 2016 1 8))]}))))

    (testing "when the period span between than a week and few months it should be P1D"
      (is (= "P1D" (default-granularity {:filters [(time-dim "current-month")]})))
      (is (= "P1D" (default-granularity {:filters [(time-dim (d 2016 1 1) (d 2016 1 9))]})))
      (is (= "P1D" (default-granularity {:filters [(time-dim (d 2016 1 1) (d 2016 2 28))]}))))

    (testing "when the period span more than a few months it should be P1M"
      (is (= "P1M" (default-granularity {:filters [(time-dim "current-year")]})))
      (is (= "P1M" (default-granularity {:filters [(time-dim (d 2016 1 1) (d 2016 4 1))]}))))))
