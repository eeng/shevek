(ns shevek.acceptance.configuration-test
  (:require [clojure.test :refer [deftest use-fixtures is testing]]
            [shevek.acceptance.test-helper :refer [wrap-acceptance-tests it login-admin click click-text visit fill fill-by-name has-css? has-text? click-tid select has-no-text?]]
            [shevek.makers :refer [make!]]
            [shevek.schemas.user :refer [User]]
            [shevek.schemas.cube :refer [Cube]]))

(use-fixtures :once wrap-acceptance-tests)

(deftest ^:acceptance configuration-tests
  (testing "users"
    (it "creating an user"
      (login-admin)
      (click-tid "sidebar-config")
      (is (has-css? ".users-list tbody tr" :count 1))
      (click-text "Create")
      (fill-by-name {"username" "bsimpson"
                     "fullname" "Bart Simpson"
                     "password" "asdf654"
                     "password-confirmation" "asdf654"})
      (click-text "Save")
      (is (has-css? ".users-list tbody tr" :count 2)))

    (it "permissions"
      (make! User {:username "mary" :fullname "Mary" :admin false})
      (make! Cube {:title "Sales"})
      (make! Cube {:title "Inventory"})
      (login-admin)
      (click-tid "sidebar-config")
      (fill :active "mary")
      (click-tid "edit")
      (click-text "Can view all cubes")
      (click {:fn/has-text "Sales"})
      (click-text "Save")
      (is (has-no-text? "Basic Information"))
      (is (has-text? "Sales"))
      (is (has-no-text? "Inventory")))))
