(ns shevek.pages.designer.actions
  (:require [shevek.rpc :as rpc]
            [shevek.i18n :refer [t]]
            [shevek.reflow.core :refer [dispatch] :refer-macros [defevh]]
            [shevek.pages.designer.helpers :refer [current-cube send-designer-query send-pinboard-queries]]
            [shevek.components.popup :refer [tooltip]]))

(defevh :designer/max-time-arrived [db cube max-time]
  (if (> max-time (get-in db [:cubes cube :max-time]))
    (-> (assoc-in db [:cubes cube :max-time] max-time)
        (send-designer-query)
        (send-pinboard-queries))))

(defevh :designer/refresh [db]
  (when-not (rpc/loading?)
    (let [cube (get-in db [:designer :report :cube])]
      (rpc/call "schema/max-time" :args [cube] :handler #(dispatch :designer/max-time-arrived cube %)))))

(defn refresh-button []
  [:button.ui.icon.default.button
   {:on-click #(dispatch :designer/refresh)
    :class (when (rpc/loading? [:designer :report-results]) "loading disabled")
    :ref (tooltip (t :actions/refresh))}
   [:i.refresh.icon]])
