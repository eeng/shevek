(ns shevek.users.repository-test
  (:require [clojure.test :refer [is]]
            [shevek.test-helper :refer [spec]]
            [shevek.makers :refer [make!]]
            [shevek.asserts :refer [without?]]
            [shevek.users.repository :refer [User find-users save-user reload]]
            [shevek.db :refer [db]]
            [bcrypt-clj.auth :refer [check-password]]))

(spec "the username should be unique"
  (make! User {:username "ddchp"})
  (is (thrown? com.mongodb.DuplicateKeyException (make! User {:username "ddchp"}))))

(spec "on create should encrypt password"
  (is (check-password "pass1234" (:password (make! User {:password "pass1234"})))))

(spec "on update should not change password if blank"
  (let [new-user (make! User {:password "pass1234" :fullname "N1"})
        changed-user (->> (assoc new-user :password "" :fullname "N2")
                          (save-user db) (reload db))]
    (is (check-password "pass1234" (:password changed-user)))))

(spec "on update should not change password if :password key is not present"
  (let [new-user (make! User {:password "pass1234" :fullname "N1"})
        changed-user (->> (dissoc new-user :password)
                          (save-user db) (reload db))]
    (is (check-password "pass1234" (:password changed-user)))))

(spec "find-users should return users sorted by username"
  (make! User {:username "a"})
  (make! User {:username "c"})
  (make! User {:username "b"})
  (is (= ["a" "b" "c"] (map :username (find-users db)))))
