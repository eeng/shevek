(ns shevek.acceptance.settings-test
  (:require [clojure.test :refer :all]
            [shevek.acceptance.test-helper :refer :all]))

(use-fixtures :once wrap-acceptance-tests)

(deftest ^:acceptance settings
  (it "allows to change language"
    (login)
    (is (has-text? "Cubes"))
    (is (has-no-text? "Cubos"))
    (click {:css "i.setting"})
    (select {:id "lang-dropdown"} "Español")
    (is (has-text? "Cubos"))
    (is (has-no-text? "Cubes"))
    (refresh)
    (is (has-text? "Cubos"))
    (click {:css "i.setting"})
    (select {:id "lang-dropdown"} "English")
    (is (has-text? "Cubes"))))
