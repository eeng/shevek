(ns shevek.lib.validation-test
  (:require-macros [cljs.test :refer [deftest testing is are]])
  (:require [pjstadig.humane-test-output]
            [shevek.lib.validation :refer [validate pred required]]))

(deftest validation-tests
  (testing "pred validator"
    (are [x y] (= x (:errors y))
      {:x ["pos"]} (validate {:x 0} [(pred :x pos? {:msg "pos"})])
      nil (validate {:x 1} [(pred :x pos? {:msg "pos"})])
      {:x ["> 0" "> 1"]} (validate {:x 0} [(pred :x #(> % 0) {:msg "> 0"})
                                           (pred :x #(> % 1) {:msg "> 1"})])))

  (testing "required validator"
    (are [x y] (= x (:errors y))
      {:x ["this field is mandatory"]} (validate {} [(required :x)])
      {:x ["this field is mandatory"]} (validate {:x " "} [(required :x)])
      nil (validate {:x "..."} [(required :x)]))))
