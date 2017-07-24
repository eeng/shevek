(ns shevek.admin.page
  (:require [shevek.i18n :refer [t]]
            [shevek.components.text :refer [page-title]]
            [shevek.admin.users :refer [users-section]]))

(defn page []
 [:div#settings.ui.container
  [page-title (t :admin/title) (t :admin/subtitle) "users"]
  [users-section]])
