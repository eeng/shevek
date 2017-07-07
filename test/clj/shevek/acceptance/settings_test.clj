(ns shevek.acceptance.settings-test
  (:require [clojure.test :refer :all]
            [shevek.acceptance.test-helper :refer :all]
            [etaoin.api :refer [refresh]]))

(deftest settings
  (it "allows to change language" page
    (login page)
    (is (has-text? page "Cube"))
    (is (has-no-text? page "Cubos"))
    (click-link page "Settings")
    (select page {:id "lang-dropdown"} "Español")
    (is (has-text? page "Cubos"))
    (is (has-no-text? page "Cube"))
    (refresh page)
    (is (has-text? page "Cubos"))))
