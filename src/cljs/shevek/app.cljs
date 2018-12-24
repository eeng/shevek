(ns shevek.app
  (:require [reagent.core :as r]
            [shevek.layout :refer [layout]]
            [shevek.schemas.interceptor :as schema]
            [shevek.reflow.core :as reflow]
            [shevek.reflow.router :refer [router]]
            [shevek.reflow.interceptors.logger :refer [logger]]
            [shevek.reflow.interceptors.recorder :refer [recorder]]
            [shevek.navigation :refer [init-navigation]]
            [shevek.lib.error]))

(defn init-reflow []
  (reflow/init (-> (router) (recorder) (logger) (schema/checker)))
  (reflow/dispatch :settings-loaded)
  (reflow/dispatch :user-restored))

(defn remove-loader []
  (-> ".preloader" js/$ (.fadeOut "slow")))

(defonce init-process
  (do
    (enable-console-print!)
    (remove-loader)
    (init-navigation)
    (init-reflow)
    :done))

(r/render-component [layout] (.getElementById js/document "app"))
