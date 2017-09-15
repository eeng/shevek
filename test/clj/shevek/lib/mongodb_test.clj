(ns shevek.lib.mongodb-test
  (:require [clojure.test :refer :all]
            [shevek.test-helper :refer [it]]
            [shevek.asserts :refer [submap?]]
            [shevek.lib.mongodb :refer [timestamp oid wrap-oids unwrap-oids]]
            [clj-time.core :refer [date-time now]]))

(deftest timestamp-tests
  (testing "should set created-at if not present"
    (with-redefs [now (constantly (date-time 2017))]
      (is (submap? {:created-at (date-time 2017)} (timestamp {})))
      (is (submap? {:created-at (date-time 2018)} (timestamp {:created-at (date-time 2018)})))))

  (testing "should always set updated-at"
    (with-redefs [now (constantly (date-time 2017))]
      (is (submap? {:updated-at (date-time 2017)} (timestamp {})))
      (is (submap? {:updated-at (date-time 2017)} (timestamp {:updated-at (date-time 2016)}))))))

(deftest wrap-oids-tests
  (testing "should wrap foreign keys in ObjectId"
    (is (= {:user-id (oid "597b7622f8d5026e49917be4")}
           (wrap-oids {:user-id "597b7622f8d5026e49917be4"})))
    (is (= {:users-ids [(oid "597b7622f8d5026e49917be4") (oid "59ba962eac09a9074dbd922a")]}
           (wrap-oids {:users-ids ["597b7622f8d5026e49917be4" "59ba962eac09a9074dbd922a"]}))))

  (testing "should wrap id in ObjectId and rename it to _id"
    (is (= {:_id (oid "597b7622f8d5026e49917be4")}
           (wrap-oids {:id "597b7622f8d5026e49917be4"}))))

  (testing "should not touch other fields"
    (is (= {:a 1 :b nil :c "d" :user-idea "x"}
           (wrap-oids {:a 1 :b nil :c "d" :user-idea "x"}))))

  (testing "should ignore foreign keys that aren't valid ObjectId"
    (is (= {:user-id nil} (wrap-oids {:user-id "asdf"})))))

(deftest unwrap-oids-tests
  (testing "should unwrap foreign keys ObjectId"
    (is (= {:user-id "597b7622f8d5026e49917be4"}
           (unwrap-oids {:user-id (oid "597b7622f8d5026e49917be4")})))
    (is (= {:users-ids ["597b7622f8d5026e49917be4" "59ba962eac09a9074dbd922a"]}
           (unwrap-oids {:users-ids [(oid "597b7622f8d5026e49917be4") (oid "59ba962eac09a9074dbd922a")]}))))

  (testing "should unwrap _id rename it to id"
    (is (= {:id "597b7622f8d5026e49917be4"}
           (unwrap-oids {:_id (oid "597b7622f8d5026e49917be4")}))))

  (testing "should not touch other fields"
    (is (= {:a 1 :b nil :c "d"}
           (unwrap-oids {:a 1 :b nil :c "d"})))))
