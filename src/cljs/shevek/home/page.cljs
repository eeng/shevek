(ns shevek.home.page
  (:require [shevek.components.text :refer [page-title]]
            [shevek.i18n :refer [t]]
            [shevek.home.dashboards :refer [dashboards-cards]]
            [shevek.home.cubes :refer [cubes-cards]]
            [shevek.home.reports :refer [reports-cards]]))

(defn page []
  [:div#home.ui.container
   [page-title (t :home/title) (t :home/subtitle) "home"]
   [:div.ui.equal.width.grid
    [cubes-cards]
    [dashboards-cards]
    [reports-cards]]])
