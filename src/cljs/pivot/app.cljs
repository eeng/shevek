(ns pivot.app
  (:require [reagent.core :as r]))

(enable-console-print!)

(defonce click-count (r/atom 0))

(defn state-ful-with-atom []
  [:div {:on-click #(swap! click-count inc)}
        "I have been clicked " @click-count " times."])

(defn init []
  (r/render-component [state-ful-with-atom]
                      (.getElementById js/document "app")))
