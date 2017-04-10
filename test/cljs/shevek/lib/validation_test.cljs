(ns shevek.lib.validation-test
  (:require-macros [cljs.test :refer [deftest testing is are]])
  (:require [pjstadig.humane-test-output]
            [shevek.lib.validation :refer [validate pred required regex email confirmation]]))

(deftest validation-tests
  (testing "general rules"
    (testing "single validator per field"
      (are [ee vr] (= ee (:errors vr))
        {:x ["pos"]} (validate {:x 0} {:x (pred pos? {:msg "pos"})})
        nil (validate {:x 1} {:x (pred pos? {:msg "pos"})})))

    (testing "multiple validators per field"
      (are [ee vr] (= ee (:errors vr))
        {:x ["> 0" "> 1"]} (validate {:x 0} {:x [(pred #(> % 0) {:msg "> 0"})
                                                 (pred #(> % 1) {:msg "> 1"})]})))

    (testing "validators are optional by default"
      (are [ee vr] (= ee (:errors vr))
        nil (validate {:x nil} {:x (pred pos? {:msg "pos"})})
        {:x ["pos"]} (validate {:x nil} {:x (pred pos? {:msg "pos" :optional? false})})))

    (testing "value interpolation in the message"
      (are [ee vr] (= ee (:errors vr))
        {:x ["val 'foo' is invalid"]}
        (validate {:x "foo"} {:x (pred #(= % "bar") {:msg "val '%s' is invalid"})}))))

  (testing "validators"
    (testing "required validator"
      (are [ee vr] (= ee (:errors vr))
        {:x ["can't be blank"]} (validate {} {:x required})
        {:x ["can't be blank"]} (validate {:x " "} {:x required})
        nil (validate {:x "..."} {:x required})))

    (testing "regex validator"
      (are [ee vr] (= ee (:errors vr))
        nil (validate {:x "foo"} {:x (regex #"foo")})
        {:x ["doesn't match pattern"]} (validate {:x "bar"} {:x (regex #"foo")})
        {:x ["oops"]} (validate {:x "bar"} {:x (regex #"foo" {:msg "oops"})})
        nil (validate {:x nil} {:x (regex #"foo")})
        {:x ["doesn't match pattern"]} (validate {:x ""} {:x (regex #"foo")})))

    (testing "email validator"
      (are [ee vr] (= ee (:errors vr))
        {:x ["is not a valid email address"]} (validate {:x "foo"} {:x email})
        nil (validate {:x "foo@bar.com"} {:x email})))

    (testing "email validator"
      (are [ee vr] (= ee (:errors vr))
        {:x ["is not a valid email address"]} (validate {:x "foo"} {:x email})
        nil (validate {:x "foo@bar.com"} {:x email})))

    (testing "confirmation validator"
      (are [ee vr] (= ee (:errors vr))
        {:y ["doesn't match the previous value"]} (validate {:x "foo" :y "foO"} {:y (confirmation :x)})
        nil (validate {:x "foo" :y "foo"} {:y (confirmation :x)})
        {:y ["not optional by default"]} (validate {:x "foo" :y nil}
                                                   {:y (confirmation :x {:msg "not optional by default"})})))))
