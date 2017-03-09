(ns pivot.app
  (:require [reagent.core :as r]
            [pivot.layout :refer [layout]]
            [pivot.dashboard :as dashboard]
            [reflow.core :as reflow :refer [dispatch]]
            [reflow.handlers :refer [identity-handler logging-handler recording-handler]]))

(defn test-page []
  [:div
   [:a {:on-click #(dispatch :add-todo "TODO")} "Add todo"]
   [:a {:on-click #(dispatch :send-rocket)} "Send rocket"]])

(defn init []
  (enable-console-print!)
  (reflow/init (-> identity-handler recording-handler logging-handler))
  (r/render-component [layout test-page]
                      (.getElementById js/document "app")))
