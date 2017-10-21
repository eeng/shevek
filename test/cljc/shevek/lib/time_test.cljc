(ns shevek.lib.time-test
  (:require #?@(:clj [[clojure.test :refer :all]
                      [clj-time.core :as t]]
                :cljs [[cljs.test :refer-macros [deftest testing is are]]
                       [cljs-time.core :as t]])
            [shevek.lib.time :refer [now date-time with-time-zone beginning-of-month parse-time to-iso8601]]))

(deftest time-zone-tests
  (testing "now"
    (with-redefs [t/now (constantly (t/date-time 2010 1 1 3))]
      (is (= "2010-01-01T03:00:00.000Z" (to-iso8601 (now))))
      #?(:clj
         (with-time-zone "America/Argentina/Buenos_Aires"
           (is (= "2010-01-01T03:00:00.000Z" (to-iso8601 (now))))))))

  (testing "date-time"
    #?(:clj
       (testing "should create in UTC by default"
         (is (= "2011-01-01T00:00:00.000Z" (to-iso8601 (date-time 2011))))
         (with-time-zone "America/Argentina/Buenos_Aires"
           (is (= "2011-01-01T03:00:00.000Z" (to-iso8601 (date-time 2011))))))
       :cljs
       (testing "time functions should be in local time zone by default"
         (is (= "2011-01-01T03:00:00.000Z" (to-iso8601 (date-time 2011)))))))

  (testing "parse-time"
    (testing "if the str contains the time part it should maintain the embedded time zone"
      (are [x y] (= x (to-iso8601 y))
        "2012-01-01T00:00:00.000Z" (parse-time "2012-01-01T00:00:00.000Z")
        "2012-01-01T03:00:00.000Z" (parse-time "2012-01-01T00:00:00.000-03:00"))
      #?(:clj
         (with-time-zone "America/Argentina/Buenos_Aires"
           (is (= "2012-01-01T00:00:00.000Z" (to-iso8601 (parse-time "2012-01-01T00:00:00.000Z")))))))

    #?(:clj
       (testing "if there is only a date should parse as UTC, unless overrided"
         (is (= "2012-01-01T00:00:00.000Z" (to-iso8601 (parse-time "2012-01-01"))))
         (with-time-zone "America/Argentina/Buenos_Aires"
           (is (= "2012-01-01T03:00:00.000Z" (to-iso8601 (parse-time "2012-01-01")))))))
    #?(:cljs
       (testing "if there is only a date should parse as local time"
         (is (= "2012-01-01T03:00:00.000Z" (to-iso8601 (parse-time "2012-01-01"))))))))
