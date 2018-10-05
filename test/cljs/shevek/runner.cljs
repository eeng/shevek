(ns shevek.runner
  (:require [doo.runner :refer-macros [doo-tests]]
            [pjstadig.humane-test-output]
            [shevek.admin.users.form-test]
            [shevek.admin.users.list-test]
            [shevek.domain.dimension]
            [shevek.lib.time.ext-test]
            [shevek.lib.validation-test]
            [shevek.schemas.conversion-test]
            [shevek.viewer.visualizations.chart-test]
            [shevek.viewer.visualizations.pivot-table-test]
            [shevek.domain.exporters.csv-test]
            [shevek.domain.pivot-table-test]))

(doo-tests
 'shevek.admin.users.form-test
 'shevek.admin.users.list-test
 'shevek.domain.dimension
 'shevek.lib.time.ext-test
 'shevek.lib.validation-test
 'shevek.schemas.conversion-test
 'shevek.viewer.visualizations.chart-test
 'shevek.viewer.visualizations.pivot-table-test
 'shevek.domain.exporters.csv-test
 'shevek.domain.pivot-table-test)
