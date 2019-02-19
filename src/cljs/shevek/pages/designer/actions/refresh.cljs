(ns shevek.pages.designer.actions.refresh
  (:require [shevek.rpc :as rpc]
            [shevek.reflow.db :as db]
            [shevek.reflow.core :refer [dispatch] :refer-macros [defevh]]
            [shevek.pages.designer.helpers :refer [send-designer-query send-pinboard-queries]]
            [shevek.components.refresh :as refresh]))

(defevh :designer/max-time-arrived [db cube max-time]
  (if (> max-time (get-in db [:cubes cube :max-time]))
    (-> (assoc-in db [:cubes cube :max-time] max-time)
        (send-designer-query)
        (send-pinboard-queries))))

(defevh :designer/refresh [db]
  (when-not (rpc/loading?) ; Do not refresh again if there is a slow query still running
    (let [cube (get-in db [:designer :report :cube])]
      (rpc/call "schema/max-time" :args [cube] :handler #(dispatch :designer/max-time-arrived cube %)))))

(defn refresh-button []
  [refresh/refresh-button {:on-refresh #(dispatch :designer/refresh)
                           :loading? #(rpc/loading? [:designer :report-results])}])
