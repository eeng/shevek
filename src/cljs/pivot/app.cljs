(ns pivot.app
  (:require [reagent.core :as r]
            [pivot.layout :refer [layout]]
            [pivot.dashboard :as dashboard]
            [reflow.core :as reflow :refer [dispatch]]
            [reflow.interceptors :as i]))

(defn test-page []
  [:div
   [:a {:on-click #(dispatch :add-todo "TODO")} "Add todo"]
   [:a {:on-click #(dispatch :send-rocket)} "Send rocket"]
   [:a {:on-click #(dispatch :no-changes)} "No changes"]])

(def event-handlers
  {:add-todo (fn [db [_ todo]] (update db :todos conj todo))
   :send-rocket (fn [db _] (update db :rocket-sent not))
   :no-changes (fn [db _] db)})

(defn init []
  (enable-console-print!)
  (reflow/init (-> (i/router event-handlers)
                  ;  (i/recorder)
                   (i/logger)))
  (r/render-component [layout test-page]
                      (.getElementById js/document "app")))
