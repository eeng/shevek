(ns shevek.app
  (:require [reagent.core :as r]
            [shevek.pages.layout :refer [layout]]
            [shevek.schemas.interceptor :as schema]
            [shevek.reflow.core :as reflow]
            [shevek.reflow.router :refer [router]]
            [shevek.reflow.interceptors.logger :refer [logger]]
            [shevek.reflow.interceptors.recorder :refer [recorder]]
            [shevek.navigation :refer [init-navigation]]
            [shevek.lib.react :refer [with-reload-mark]]
            [shevek.lib.error])) ; So the error handlers are registered

(defn init-reflow []
  (reflow/init (-> (router) (recorder) (logger) (schema/checker)))
  (reflow/dispatch :preferences/loaded)
  (reflow/dispatch :sessions/restore))

(defn remove-loader []
  (-> ".preloader" js/$ (.fadeOut "slow")))

(defn- mount-root []
  (r/render-component [layout] (.getElementById js/document "app")))

(defn reload []
  (with-reload-mark mount-root))

(defn ^:export main []
  (enable-console-print!)
  (remove-loader)
  (init-navigation)
  (init-reflow)
  (mount-root))
