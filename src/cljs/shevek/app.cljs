(ns shevek.app
  (:require [reagent.core :as r]
            [shevek.layout :refer [layout]]
            [shevek.schemas.interceptor :as schema]
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
  (reflow/init (-> (i/router) (i/logger) (schema/checker)))
  (reflow/dispatch :settings-loaded)
  (reflow/dispatch :user-restored)
  (r/render-component [layout] (.getElementById js/document "app")))
