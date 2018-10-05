(ns shevek.admin.users.form-test
  (:require [cljs.test :refer-macros [deftest testing is]]
            [shevek.asserts :refer [error-on? no-error-on?]]
            [shevek.pages.admin.users.form :refer [user-validations adapt-for-client adapt-for-server]]
            [shevek.lib.validation :as v]
            [shevek.domain.cubes :refer [cubes-list]]
            [shevek.lib.time :refer [date-time]]))

(defn validate-user [user]
  (v/validate user user-validations))

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
    (is (no-error-on? :password (validate-user {:id 1 :password ""})))
    (is (error-on? :password (validate-user {:id 1 :password "asd123"}))))

  (testing "password-confirmation should be equal to password, when present"
    (is (error-on? :password-confirmation (validate-user {:password "aaA" :password-confirmation "aaa"})))
    (is (no-error-on? :password-confirmation (validate-user {:password "aaA" :password-confirmation "aaA"})))
    (is (no-error-on? :password-confirmation (validate-user {}))))

  (testing "email is optional but with correct format"
    (is (no-error-on? :email (validate-user {})))
    (is (error-on? :email (validate-user {:email "nop"})))
    (is (no-error-on? :email (validate-user {:email "nop@acme.com"})))))

(deftest adapt-for-client-tests
  (testing "when all cubes are visible should add the available cubes to the user as not selected"
    (with-redefs [cubes-list (constantly [{:name "c1"}])]
      (is (= {:allowed-cubes "all"
              :only-cubes-selected false
              :cubes [{:name "c1" :selected false :only-measures-selected false :allowed-measures nil :filters []}]}
             (adapt-for-client {:allowed-cubes "all"})))))

  (testing "when only certain cubes are visible, should mark the corresponding available cube as selected"
    (with-redefs [cubes-list (constantly [{:name "c1"} {:name "c2"}])]
      (is (= {:allowed-cubes [{:name "c1" :measures "all"}]
              :only-cubes-selected true
              :cubes [{:name "c1" :selected true :only-measures-selected false :allowed-measures nil :filters []}
                      {:name "c2" :selected false :only-measures-selected false :allowed-measures nil :filters []}]}
             (adapt-for-client {:allowed-cubes [{:name "c1" :measures "all"}]})))))

  (testing "with only certain measures are visible"
    (with-redefs [cubes-list (constantly [{:name "c1" :title "C1"}])]
      (is (= [{:name "c1" :title "C1" :selected true :only-measures-selected true :allowed-measures ["m1"] :filters []}]
             (:cubes (adapt-for-client {:allowed-cubes [{:name "c1" :measures ["m1"]}]}))))))

  (testing "with filters are applied, should expand them with the title from the corresponding available cubes"
    (with-redefs [cubes-list (constantly [{:name "c1" :dimensions [{:name "d" :title "D"}]}])]
      (is (= [{:name "d" :operator "include" :value #{"v"} :title "D"}]
             (-> (adapt-for-client {:allowed-cubes [{:name "c1" :filters [{:name "d" :operator "include" :value ["v"]}]}]})
                 :cubes first :filters))))))

(deftest adapt-for-server-tests
  (testing "should remove unwanted keys"
    (is (= {:password "secret" :allowed-cubes "all"}
           (adapt-for-server {:password "secret" :password-confirmation "secret"
                              :cubes [{}] :only-cubes-selected false}))))

  (testing "should keep the selected cubes"
    (is (= {:allowed-cubes [{:name "c2" :measures "all" :filters []}]}
           (adapt-for-server {:only-cubes-selected true
                              :cubes [{:name "c1" :selected false} {:name "c2" :selected true}]}))))

  (testing "should simplify the filters"
    (is (= [{:name "d" :operator "include" :value ["v"]}
            {:name "t" :period "latest-day"}
            {:name "a" :interval ["2018-04-04T03:00:00.000Z" "2018-04-06T02:59:59.999Z"]}]
           (-> (adapt-for-server {:only-cubes-selected true
                                  :cubes [{:name "c1" :selected true
                                           :filters [{:name "d" :operator "include" :value #{"v"} :title "D" :type "..."}
                                                     {:name "t" :title "T" :period "latest-day"}
                                                     {:name "a" :title "A" :interval [(date-time 2018 4 4) (date-time 2018 4 5)]}]}]})
               :allowed-cubes first :filters)))))
