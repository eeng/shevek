(ns shevek.lib.mongodb-test
  (:require [clojure.test :refer [deftest testing is use-fixtures]]
            [shevek.test-helper :refer [it wrap-unit-tests]]
            [shevek.asserts :refer [submap?]]
            [shevek.lib.mongodb :as m :refer [timestamp oid wrap-oids unwrap-oids]]
            [clj-time.core :refer [date-time now]]
            [shevek.db :refer [db]]))

(use-fixtures :once wrap-unit-tests)

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
           (wrap-oids {:users-ids ["597b7622f8d5026e49917be4" "59ba962eac09a9074dbd922a"]})))
    (is (= {:users [{:user-id (oid "597b7622f8d5026e49917be4")}]}
           (wrap-oids {:users [{:user-id "597b7622f8d5026e49917be4"}]}))))

  (testing "should wrap id in ObjectId and rename it to _id"
    (is (= {:_id (oid "597b7622f8d5026e49917be4")}
           (wrap-oids {:id "597b7622f8d5026e49917be4"}))))

  (testing "should not touch other fields"
    (is (= {:a 1 :b nil :c "d" :user-idea "x" :tags ["a"]}
           (wrap-oids {:a 1 :b nil :c "d" :user-idea "x" :tags ["a"]}))))

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
    (is (= {:a 1 :b nil :c "d" :tags ["a"]}
           (unwrap-oids {:a 1 :b nil :c "d" :tags ["a"]})))))

(deftest find-by-id
  (it "wraps and unwraps the ids"
    (let [{:keys [id]} (m/save db "docs" {:title "t"})]
      (is (string? id))
      (is (submap? {:id id :title "t"} (m/find-by-id db "docs" id)))))

  (it "with invalid ids returns nil"
    (is (nil? (m/find-by-id db "docs" "invalid"))))

  (it "if the document doesn't exists returns nil"
    (is (nil? (m/find-by-id db "docs" "597b7622f8d5026e49917be4")))))
