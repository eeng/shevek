(ns shevek.admin.page
  (:require [shevek.i18n :refer [t]]
            [shevek.components :refer [page-title]]
            [shevek.admin.users :refer [users-section]]
            [shevek.admin.schema :refer [schema-section]]))

(defn page []
 [:div#settings.ui.container
  [page-title (t :admin/title) (t :admin/subtitle) "settings"]
  [users-section]
  [schema-section]])