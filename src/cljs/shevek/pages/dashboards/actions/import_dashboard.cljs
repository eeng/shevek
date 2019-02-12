(ns shevek.pages.dashboards.actions.import-dashboard
  (:require [shevek.reflow.core :refer [dispatch] :refer-macros [defevh]]
            [shevek.i18n :refer [t]]))

(defn import-button [dashboard]
  [:button.ui.green.labeled.icon.button
   {:on-click #(js/console.log "YEA")}
   [:i.cloud.download.icon]
   (t :dashboard/import)])
