(ns shevek.lib.time-test
  (:require #?@(:clj [[clojure.test :refer :all]
                      [clj-time.core :as t]]
                :cljs [[cljs.test :refer-macros [deftest testing is are]]
                       [cljs-time.core :as t]])
            [shevek.lib.time :refer [now date-time with-time-zone beginning-of-month parse-time to-iso8601]]))

(deftest time-zone-tests
  #?(:clj
     (testing "time functions should be in UTC by default"
       (with-redefs [t/now (constantly (t/date-time 2010))]
         (are [x y] (= x (to-iso8601 y))
           "2010-01-01T00:00:00.000Z" (now)
           "2011-01-01T00:00:00.000Z" (date-time 2011)
           "2012-01-01T00:00:00.000Z" (parse-time "2012-01-01"))))
     :cljs
      (testing "time functions should be in local time zone by default"
        (with-redefs [t/now (constantly (t/date-time 2010 1 1 3))]
          (are [x y] (= x (to-iso8601 y))
            "2010-01-01T00:00:00.000" (now)
            "2011-01-01T00:00:00.000" (date-time 2011)
            "2012-01-01T00:00:00.000" (parse-time "2012-01-01T03:00Z")))))

  #?(:clj
     (testing "functions should allow to temporarly override the time zone they use"
       (with-redefs [t/now (constantly (t/date-time 2010 1 1 3))]
         (with-time-zone "America/Argentina/Buenos_Aires"
           (are [x y] (= x (to-iso8601 y))
             "2010-01-01T03:00:00.000Z" (now)
             "2011-01-01T03:00:00.000Z" (date-time 2011)
             "2012-01-01T03:00:00.000Z" (beginning-of-month (date-time 2012 1 10))
             "2013-01-01T03:00:00.000Z" (parse-time "2013-01-01")))))))
