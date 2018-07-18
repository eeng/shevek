(ns shevek.pages.home.page
  (:require [shevek.components.text :refer [page-title]]
            [shevek.i18n :refer [t]]
            [shevek.pages.home.dashboards :refer [dashboards-cards]]
            [shevek.pages.home.cubes :refer [cubes-cards]]
            [shevek.pages.home.reports :refer [reports-cards]]))

(defn page []
  [:div#home.ui.container
   [page-title (t :home/title) (t :home/subtitle) "home"]
   [:div.ui.equal.width.relaxed.grid
    [cubes-cards]
    [reports-cards]
    [dashboards-cards]]])
