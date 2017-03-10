(ns pivot.app
  (:require-macros [reflow.macros :refer [defevh]])
  (:require [reagent.core :as r]
            [pivot.layout :refer [layout]]
            [pivot.dashboard :as dashboard]
            [reflow.core :as reflow :refer [dispatch]]
            [reflow.interceptors :as i]))

(defevh :no-changes [db event]
  db)

(defevh :add-todo [db [_ todo]]
  (update db :todos conj todo))

(defevh :send-rocket [db _]
  (update db :rocket-sent not))

(defn test-page []
  [:div
   [:a {:on-click #(dispatch :add-todo "TODO")} "Add todo"]
   [:a {:on-click #(dispatch :send-rocket)} "Send rocket"]
   [:a {:on-click #(dispatch :no-changes)} "No changes"]])

(defn init []
  (enable-console-print!)
  (reflow/init (-> (i/router)
                   (i/logger)))
  (r/render-component [layout test-page]
                      (.getElementById js/document "app")))
