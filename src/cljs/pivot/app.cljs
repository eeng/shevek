(ns pivot.app
  (:require [reagent.core :as r]
            [pivot.layout :refer [layout]]
            [pivot.dashboard :as dashboard]
            [reflow.core :as reflow]
            [reflow.interceptors :as i]))

(defn init []
  (enable-console-print!)
  (reflow/init (-> (i/router) (i/logger)))
  (r/render-component [layout dashboard/page]
                      (.getElementById js/document "app")))
