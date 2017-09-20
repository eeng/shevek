(ns shevek.navigation
  (:require [secretary.core :as secretary :refer-macros [defroute]]
            [pushy.core :as pushy]
            [shevek.reflow.core :refer [dispatch] :refer-macros [defevh]]
            [shevek.reflow.db :as db]))

(defonce history
  (pushy/pushy secretary/dispatch!
               (fn [x] (when (secretary/locate-route x) x))))

(defn init-navigation []
  (secretary/set-config! :prefix "/")
  (pushy/start! history))

(defn current-page []
  (db/get :page))

(defn current-page? [page]
  (= (current-page) page))

(defn navigate [& args]
  (pushy/set-token! history (apply str args)))

(defn get-url []
  (pushy/get-token history))

(defn set-url [next-url]
  (let [current-url (get-url)]
    (when (not= next-url current-url)
      (.pushState js/history {}, nil, next-url))))

(defevh :navigate [db page]
  (assoc db :page page))

(defroute "/viewer/:encoded-report" [encoded-report]
  (dispatch :viewer-restored encoded-report))

(defroute "/admin" []
  (dispatch :navigate :admin))

(defroute "/account" []
  (dispatch :navigate :account))

(defroute "/dashboard/:id" [id]
  (dispatch :dashboard-selected id))

(defroute "*" []
  (dispatch :navigate :home))
