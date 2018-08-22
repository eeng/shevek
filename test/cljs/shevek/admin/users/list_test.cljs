(ns shevek.admin.users.list-test
  (:require [cljs.test :refer-macros [deftest testing is use-fixtures]]
            [shevek.support.db :refer-macros [with-app-db]]
            [shevek.support.reagent :refer [with-container render-component text]]
            [shevek.pages.admin.users.list :as l]))

(defn permissions-text [args]
  (render-component [l/permissions-text args])
  (text ".permissions"))

(deftest permissions-text-tests
  (testing "when can view all cubes"
    (is (= "Can view all cubes" (permissions-text {:allowed-cubes "all"}))))

  (testing "when can only view some cubes"
    (with-app-db {:cubes {"c1" {:title "C1"} "c2" {:title "C2"}}}
      (is (= "Can view only the following cubes: C1, C2"
             (permissions-text {:allowed-cubes [{:name "c1" :measures "all"} {:name "c2"}]})))))

  (testing "when can only view some measures"
    (with-app-db {:cubes {"c1" {:title "C1" :measures [{:name "m1" :title "M1"} {:name "m2" :title "M2"}]}}}
      (is (= "Can view only the following cubes: C1 [Measures: M1, M2]"
             (permissions-text {:allowed-cubes [{:name "c1" :measures ["m1" "m2"]}]})))))

  (testing "when filters are applied"
    (with-app-db {:cubes {"c1" {:title "C1" :dimensions [{:name "d1" :title "D1"}]}}}
      (is (= "Can view only the following cubes: C1 [D1 (v1)]"
             (permissions-text {:allowed-cubes [{:name "c1" :filters [{:name "d1" :operator "include" :value ["v1"]}]}]}))))))

(use-fixtures :each with-container)
