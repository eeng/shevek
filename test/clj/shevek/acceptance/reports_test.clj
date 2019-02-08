(ns shevek.acceptance.reports-test
  (:require [clojure.test :refer [deftest use-fixtures is]]
            [shevek.acceptance.test-helper :refer [wrap-acceptance-tests it click click-link fill has-css? click-tid has-text? wait-exists refresh]]))

(use-fixtures :once wrap-acceptance-tests)

(deftest ^:acceptance reports)
