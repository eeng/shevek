(ns pivot.app
  (:require [reagent.core :as r]
            [pivot.layout :refer [layout]]
            [pivot.dashboard :as dashboard]
            [reflow.core :as reflow :refer [dispatch]]
            [reflow.handlers :refer [router logging-handler recording-handler]]))

(defn test-page []
  [:div
   [:a {:on-click #(dispatch :add-todo "TODO")} "Add todo"]
   [:a {:on-click #(dispatch :send-rocket)} "Send rocket"]])

(def event-handlers
  {:add-todo (fn [state [_ todo]] (update state :todos conj todo))
   :send-rocket (fn [state _] (update state :rocket-sent not))})

(defn init []
  (enable-console-print!)
  (reflow/init (-> (router event-handlers)
                   (recording-handler)
                   (logging-handler)))
  (r/render-component [layout test-page]
                      (.getElementById js/document "app")))
