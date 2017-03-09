(ns pivot.app
  (:require [reagent.core :as r]
            [pivot.layout :refer [layout]]
            [pivot.dashboard :as dashboard]))

(defn init []
  (enable-console-print!)
  (r/render-component [layout dashboard/page]
                      (.getElementById js/document "app")))
