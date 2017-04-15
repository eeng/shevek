(ns shevek.notification
  (:require [reagent.core :as r]))

(defonce notification-data (r/atom {}))

(defn notify [message & {:keys [timeout] :or {timeout 5000} :as opts}]
  (reset! notification-data (assoc opts :message message :showed? true))
  (js/setTimeout #(swap! notification-data assoc :showed? false) timeout))

(defn notification []
  (let [{:keys [showed? message]} @notification-data]
    [:div#notification
     {:class (if showed? "visible" "hidden")}
     message]))
