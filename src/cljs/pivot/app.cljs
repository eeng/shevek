(ns pivot.app
  (:require [reagent.core :as r]
            [pivot.layout :refer [layout]]
            [pivot.settings :refer [load-settings]]
            [reflow.core :as reflow]
            [reflow.interceptors :as i]
            [secretary.core :as secretary]
            [goog.events :as events]
            [goog.history.EventType :as EventType])
  (:import goog.History))

(defonce history
  (doto (History.)
    (events/listen EventType/NAVIGATE #(secretary/dispatch! (.-token %)))
    (.setEnabled true)))

(defn init []
  (enable-console-print!)
  (reflow/init (load-settings) (-> (i/router) (i/logger)))
  (r/render-component [layout] (.getElementById js/document "app")))
