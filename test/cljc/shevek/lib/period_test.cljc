(ns shevek.lib.period-test
  (:require #?(:clj [clojure.test :refer :all]
               :cljs [cljs.test :refer-macros [deftest testing is]])
            [shevek.lib.period :refer [to-interval]]
            [shevek.lib.time :refer [now date-time]]))

(def d date-time)

(deftest to-interval-test []
  (testing "period latest-hour"
    (is (= [(d 2017 3 6 16 28) (d 2017 3 6 17 28)]
           (to-interval "latest-hour" (d 2017 3 6 17 27 30)))))

  (testing "period latest-day"
    (is (= [(d 2017 3 5 17 29) (d 2017 3 6 17 29)]
           (to-interval "latest-day" (d 2017 3 6 17 28))))
    (is (= [(d 2017 1 2) (d 2017 1 3)]
           (to-interval "latest-day" (d 2017 1 2 23 59)))))

  (testing "period current-day"
    (with-redefs [now (constantly (d 2017 3 15 18 11))]
      (is (= [(d 2017 3 15) (d 2017 3 15 23 59 59 999)]
             (to-interval "current-day" nil)))))

  (testing "period current-week"
    (with-redefs [now (constantly (d 2017 3 20 15 10))]
      (is (= [(d 2017 3 20) (d 2017 3 26 23 59 59 999)]
             (to-interval "current-week" nil))))
    (with-redefs [now (constantly (d 2017 3 22 23 59))]
      (is (= [(d 2017 3 20) (d 2017 3 26 23 59 59 999)]
             (to-interval "current-week" nil))))
    (with-redefs [now (constantly (d 2017 3 26 10 59))]
      (is (= [(d 2017 3 20) (d 2017 3 26 23 59 59 999)]
             (to-interval "current-week" nil))))
    (with-redefs [now (constantly (d 2017 3 27 0 1))]
      (is (= [(d 2017 3 27) (d 2017 4 2 23 59 59 999)]
             (to-interval "current-week" nil)))))

  (testing "period current-month"
    (with-redefs [now (constantly (d 2017 3 15 18 11))]
      (is (= [(d 2017 3 1) (d 2017 3 31 23 59 59 999)]
             (to-interval "current-month" nil)))))

  (testing "period current-quarter"
    (with-redefs [now (constantly (d 2017 3 15))]
      (is (= [(d 2017 1 1) (d 2017 3 31 23 59 59 999)]
             (to-interval "current-quarter" nil))))
    (with-redefs [now (constantly (d 2017 4 3))]
      (is (= [(d 2017 4 1) (d 2017 6 30 23 59 59 999)]
             (to-interval "current-quarter" nil))))
    (with-redefs [now (constantly (d 2017 12 31))]
      (is (= [(d 2017 10 1) (d 2017 12 31 23 59 59 999)]
             (to-interval "current-quarter" nil))))
    (with-redefs [now (constantly (d 2018 1 2))]
      (is (= [(d 2018 1 1) (d 2018 3 31 23 59 59 999)]
             (to-interval "current-quarter" nil)))))

  (testing "period current-year"
    (with-redefs [now (constantly (d 2017 3 15))]
      (is (= [(d 2017 1 1) (d 2017 12 31 23 59 59 999)]
             (to-interval "current-year" nil))))
    (with-redefs [now (constantly (d 2018 6 2))]
      (is (= [(d 2018 1 1) (d 2018 12 31 23 59 59 999)]
             (to-interval "current-year" nil)))))

  (testing "period previous-day"
    (with-redefs [now (constantly (d 2017 3 15 18 11))]
      (is (= [(d 2017 3 14) (d 2017 3 14 23 59 59 999)]
             (to-interval "previous-day" nil)))))

  (testing "period previous-week"
    (with-redefs [now (constantly (d 2017 3 20 15 10))]
      (is (= [(d 2017 3 13) (d 2017 3 19 23 59 59 999)]
             (to-interval "previous-week" nil))))
    (with-redefs [now (constantly (d 2017 3 24 8 10))]
      (is (= [(d 2017 3 13) (d 2017 3 19 23 59 59 999)]
             (to-interval "previous-week" nil)))))

  (testing "period previous-month"
    (with-redefs [now (constantly (d 2017 3 15 18 11))]
      (is (= [(d 2017 2 1) (d 2017 2 28 23 59 59 999)]
             (to-interval "previous-month" nil))))
    (with-redefs [now (constantly (d 2017 1 15 18 11))]
      (is (= [(d 2016 12 1) (d 2016 12 31 23 59 59 999)]
             (to-interval "previous-month" nil)))))

  (testing "period previous-quarter"
    (with-redefs [now (constantly (d 2017 3 15))]
      (is (= [(d 2016 10 1) (d 2016 12 31 23 59 59 999)]
             (to-interval "previous-quarter" nil))))
    (with-redefs [now (constantly (d 2017 4 3))]
      (is (= [(d 2017 1 1) (d 2017 3 31 23 59 59 999)]
             (to-interval "previous-quarter" nil)))))

  (testing "period previous-year"
    (with-redefs [now (constantly (d 2017 3 15))]
      (is (= [(d 2016 1 1) (d 2016 12 31 23 59 59 999)]
             (to-interval "previous-year" nil))))))
