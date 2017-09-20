(ns shevek.app
  (:require [reagent.core :as r]
            [shevek.layout :refer [layout]]
            [shevek.schemas.interceptor :as schema]
            [shevek.reflow.core :as reflow]
            [shevek.reflow.interceptors :as i]
            [shevek.lib.error]
            [secretary.core :as secretary]
            [goog.events :as events]
            [goog.history.EventType :as EventType]
            [cljsjs.jquery])
  (:import goog.History))

(defonce history
  (doto (History.)
    (events/listen EventType/NAVIGATE #(secretary/dispatch! (.-token %)))
    (.setEnabled true)))

(defn init-reflow []
  (reflow/init (-> (i/router) (i/logger) (schema/checker)))
  (reflow/dispatch :settings-loaded)
  (reflow/dispatch :user-restored))

(defn init []
  (enable-console-print!)
  (init-reflow)
  (r/render-component [layout] (.getElementById js/document "app")))
