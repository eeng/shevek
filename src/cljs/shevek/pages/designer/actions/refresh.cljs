(ns shevek.pages.designer.actions.refresh
  (:require [reagent.core :as r]
            [shevek.rpc :as rpc]
            [shevek.i18n :refer [t]]
            [shevek.reflow.db :as db]
            [shevek.reflow.core :refer [dispatch] :refer-macros [defevh]]
            [shevek.pages.designer.helpers :refer [current-cube send-designer-query send-pinboard-queries]]
            [shevek.components.popup :refer [show-popup close-popup tooltip]]
            [shevek.components.form :refer [select]]
            [cuerdas.core :as str]))

(defevh :designer/max-time-arrived [db cube max-time]
  (if (> max-time (get-in db [:cubes cube :max-time]))
    (-> (assoc-in db [:cubes cube :max-time] max-time)
        (send-designer-query)
        (send-pinboard-queries))))

(defevh :designer/refresh [db]
  (when-not (rpc/loading?)
    (let [cube (get-in db [:designer :report :cube])]
      (rpc/call "schema/max-time" :args [cube] :handler #(dispatch :designer/max-time-arrived cube %)))))

(defonce auto-refresh-interval (atom nil))

(defn- clear-auto-refresh-interval! []
  (js/clearTimeout @auto-refresh-interval))

(defn- set-auto-refresh-interval! [every on-refresh]
  (clear-auto-refresh-interval!)
  (when (and every (pos? every))
    (reset! auto-refresh-interval (js/setInterval on-refresh (* 1000 every)))))

(defn- popup-content [every on-refresh]
  [:div#settings-popup.ui.form
   [:div.field
    [:label (t :preferences/refresh-every)]
    [select (t :preferences/refresh-every-opts)
     {:selected every
      :on-change #(let [new-every (str/parse-int %)]
                    (set-auto-refresh-interval! new-every on-refresh)
                    (dispatch :preferences/save {:auto-refresh new-every})
                    (close-popup))}]]
   [:button.ui.fluid.primary.button
    {:on-click on-refresh
     :class (when (rpc/loading? [:designer :report-results]) "loading disabled")}
    (t :preferences/refresh-now)]])

(defn refresh-button []
  (r/with-let [on-refresh #(dispatch :designer/refresh)
               _ (set-auto-refresh-interval! (db/get-in [:preferences :auto-refresh]) on-refresh)]
    (let [every (db/get-in [:preferences :auto-refresh])] ; Repetead to get the updated value when the user changes it
      [:button.ui.default.icon.button
       {:on-click #(show-popup % [popup-content every on-refresh] {:position "bottom right"})}
       (when (pos? every)
         [:span.current-refresh
           (t :preferences/refresh-every) " "
           (get (t :preferences/refresh-every-opts) every)])
       [:i.refresh.icon {:class (when (rpc/loading? [:designer :report-results]) "loading")}]])
    (finally
      (clear-auto-refresh-interval!))))
