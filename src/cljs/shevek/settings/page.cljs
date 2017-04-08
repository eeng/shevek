(ns shevek.settings.page
  (:require [shevek.i18n :refer [t]]
            [shevek.components :refer [page-title]]
            [shevek.settings.regional :refer [regional-section]]
            [shevek.settings.users :refer [users-section]]
            [shevek.settings.schema :refer [schema-section]]))

(defn page []
 [:div#settings.ui.container
  [page-title (t :settings/title) (t :settings/subtitle) "settings"]
  [regional-section]
  [users-section]
  [schema-section]])
