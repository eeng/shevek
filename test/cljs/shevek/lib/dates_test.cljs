(ns shevek.lib.dates-test
  (:require-macros [cljs.test :refer [deftest testing is are]])
  (:require [shevek.lib.dates :as d]
            [cljs-time.core :as t]))

(deftest time-zone-tests
  (testing "all creation functions return datetimes in local time zone"
    (with-redefs [t/now (constantly (t/date-time 2010 1 1 3 0))]
      (is (= (t/local-date-time 2010 1 1 0 0) (d/now)))
      (is (= (t/local-date-time 2009 12 31 0 0) (d/yesterday)))
      (is (= (t/local-date-time 2010 1 1 23 59 59 999) (d/end-of-day (d/now))))
      (is (= (t/local-date-time 2010 1 31 23 59 59 999) (d/end-of-month (d/now))))))

  (testing "parse-time takes an iso-8601 string and returns datetime in local time zone"
    (is (= (t/local-date-time 2017 7 13 17 5 32 173) (d/parse-time "2017-07-13T20:05:32.173Z")))
    (is (= (t/local-date-time 2017 7 13 20 5 32 173) (d/parse-time "2017-07-13T20:05:32.173-03:00"))))

  (testing "parse-date takes a string and returns datetime in local time zone"
    (is (= (t/local-date-time 2017 7 13) (d/parse-date "2017-07-13"))))

  ; We could just send the datetime in local time zone but cljs-time doesn't add the offset. Besides this way in the server we only use u
  (testing "to-iso8601 formats back to UTC"
    (is (= "2010-01-01T03:00:00.000Z" (d/to-iso8601 (t/local-date-time 2010 1 1 0 0))))))
