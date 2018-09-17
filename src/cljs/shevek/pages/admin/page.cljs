(ns shevek.pages.admin.page
  (:require [shevek.i18n :refer [t]]
            [shevek.components.text :refer [page-title]]
            [shevek.pages.admin.users.list :refer [users-section]]))

(defn page []
 [:div#admin.ui.container
  [page-title (t :admin/title) (t :admin/subtitle) "users"]
  [users-section]])