(ns shevek.runner
  (:require [doo.runner :refer-macros [doo-tests]]
            [pjstadig.humane-test-output]
            [cljsjs.jquery]
            [shevek.pages.configuration.users.form-test]
            [shevek.pages.configuration.users.list-test]
            [shevek.domain.dimension]
            [shevek.lib.time.ext-test]
            [shevek.lib.validation-test]
            [shevek.schemas.conversion-test]
            [shevek.pages.designer.visualizations.chart-test]
            [shevek.pages.designer.visualizations.pivot-table-test]
            [shevek.domain.exporters.csv-test]
            [shevek.domain.pivot-table-test]))

(doo-tests
 'shevek.pages.configuration.users.form-test
 'shevek.pages.configuration.users.list-test
 'shevek.domain.dimension
 'shevek.lib.time.ext-test
 'shevek.lib.validation-test
 'shevek.schemas.conversion-test
 'shevek.pages.designer.visualizations.chart-test
 'shevek.pages.designer.visualizations.pivot-table-test
 'shevek.domain.exporters.csv-test
 'shevek.domain.pivot-table-test)
