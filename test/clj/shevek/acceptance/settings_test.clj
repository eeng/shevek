(ns shevek.acceptance.settings-test
  (:require [clojure.test :refer :all]
            [shevek.acceptance.test-helper :refer :all]
            [etaoin.api :refer :all]))

(deftest settings
  (it "allows to change language" page
    (visit page "/")
    (is (has-text? page "Cube"))
    (is (not (has-text? page "Cubos")))
    (click-link page "Settings")
    (select page {:id "lang-dropdown"} "Español")
    (is (has-text? page "Cubos"))
    (is (not (has-text? page "Cube")))
    (refresh page)
    (is (has-text? page "Cubos"))))
