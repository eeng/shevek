(ns shevek.settings.users-test
  (:require-macros [cljs.test :refer [deftest testing is are]])
  (:require [pjstadig.humane-test-output]
            [shevek.asserts :refer [error-on? no-error-on?]]
            [shevek.settings.users :refer [validate-user]]))

(deftest validate-user-tests
  (testing "username is required"
    (is (error-on? :username (validate-user {}))))

  (testing "fullname is required"
    (is (error-on? :fullname (validate-user {}))))

  (testing "password should be strong enough"
    (is (error-on? :password (validate-user {})))
    (is (error-on? :password (validate-user {:password "asd123"})))
    (is (no-error-on? :password (validate-user {:password "asdf123"}))))

  (testing "password is required only on new users but if entered should be strong"
    (is (no-error-on? :password (validate-user {:_id 1 :password ""})))
    (is (error-on? :password (validate-user {:_id 1 :password "asd123"}))))

  (testing "password-confirmation should be equal to password, when present"
    (is (error-on? :password-confirmation (validate-user {:password "aaA" :password-confirmation "aaa"})))
    (is (no-error-on? :password-confirmation (validate-user {:password "aaA" :password-confirmation "aaA"})))
    (is (no-error-on? :password-confirmation (validate-user {}))))

  (testing "email is optional but with correct format"
    (is (no-error-on? :email (validate-user {})))
    (is (error-on? :email (validate-user {:email "nop"})))
    (is (no-error-on? :email (validate-user {:email "nop@acme.com"})))))
