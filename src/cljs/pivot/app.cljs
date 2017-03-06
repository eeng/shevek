(ns pivot.app
  (:require [reagent.core :as r]
            [pivot.layout :refer [layout]]
            [pivot.dashboard :as dashboard]))

(enable-console-print!)

(defn init []
  (r/render-component [layout dashboard/page]
                      (.getElementById js/document "app")))
