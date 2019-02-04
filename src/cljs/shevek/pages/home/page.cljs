(ns shevek.pages.home.page
  (:require [shevek.i18n :refer [t]]
            [shevek.components.layout :refer [page-with-header panel]]
            [shevek.pages.cubes.list :refer [cubes-list]]
            [shevek.pages.reports.list :refer [reports-list]]
            [shevek.pages.dashboards.list :refer [dashboards-list]]))

(defn page []
  [page-with-header
   {:title (t :home/title) :subtitle (t :home/subtitle) :icon "home" :id "home"}
   [:div.ui.equal.width.grid
    [:div.column
     [panel {:title (t :cubes/title)}
      [cubes-list]]]
    [:div.column
     [panel {:title (t :dashboards/title)}
      [dashboards-list]]]
    [:div.column
     [panel {:title (t :reports/title)}
      [reports-list]]]]])
