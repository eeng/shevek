(ns shevek.users.repository-test
  (:require [clojure.test :refer [deftest is]]
            [shevek.test-helper :refer [it]]
            [shevek.makers :refer [make!]]
            [shevek.asserts :refer [without?]]
            [shevek.users.repository :refer [User find-users save-user reload]]
            [shevek.db :refer [db]]
            [buddy.hashers :as hashers]))

(deftest save-user-tests
  (it "should throw error if username already exists"
    (make! User {:username "ddchp"})
    (is (thrown? com.mongodb.DuplicateKeyException (make! User {:username "ddchp"}))))

  (it "on create should encrypt password"
    (is (hashers/check "pass1234" (:password (make! User {:password "pass1234"})))))

  (it "on update should not change password if blank"
    (let [new-user (make! User {:password "pass1234" :fullname "N1"})
          changed-user (->> (assoc new-user :password "" :fullname "N2")
                            (save-user db) (reload db))]
      (is (hashers/check "pass1234" (:password changed-user)))))

  (it "on update should not change password if :password key is not present"
    (let [new-user (make! User {:password "pass1234" :fullname "N1"})
          changed-user (->> (dissoc new-user :password)
                            (save-user db) (reload db))]
      (is (hashers/check "pass1234" (:password changed-user))))))

(deftest find-users-tests
  (it "should return users sorted by username"
    (make! User {:username "a"})
    (make! User {:username "c"})
    (make! User {:username "b"})
    (is (= ["a" "b" "c"] (map :username (find-users db)))))

  (it "should no return passwords"
    (make! User {:username "a"})
    (is (without? :password (first (find-users db))))))
