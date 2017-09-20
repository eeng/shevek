(ns shevek.app
  (:require [reagent.core :as r]
            [shevek.layout :refer [layout]]
            [shevek.schemas.interceptor :as schema]
            [shevek.reflow.core :as reflow]
            [shevek.reflow.interceptors :as i]
            [shevek.lib.error]
            [shevek.navigation :refer [init-navigation]]
            [cljsjs.jquery]))

(defn init-reflow []
  (reflow/init (-> (i/router) (i/logger) (schema/checker)))
  (reflow/dispatch :settings-loaded)
  (reflow/dispatch :user-restored))

(defn init []
  (enable-console-print!)
  (init-navigation)
  (init-reflow)
  (r/render-component [layout] (.getElementById js/document "app")))
