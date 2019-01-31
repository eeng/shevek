(ns shevek.pages.configuration.page
  (:require [shevek.components.layout :refer [page-with-header]]
            [shevek.i18n :refer [t]]
            [shevek.pages.configuration.users.list :refer [users-section]]))

(defn page []
  [page-with-header
   {:title (t :configuration/title) :subtitle (t :configuration/subtitle) :icon "setting" :id "configuration"}
   [:div.ui.secondary.menu
    [:a.item.active {:href "/configuration"}
     [:i.users.icon]
     (t :configuration/users)]]
   [:div.ui.tab.active
    [users-section]]])
