(ns shevek.navigation
  (:require [secretary.core :as secretary :refer-macros [defroute]]
            [pushy.core :as pushy]
            [shevek.reflow.core :refer [dispatch] :refer-macros [defevh]]
            [shevek.reflow.db :as db]
            [shevek.lib.error :refer [show-page-404]]))

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

(defn active-class-when-page [& pages]
  (when (some current-page? pages) "active"))

(defn set-url
  "Changes the URL without dispatching the routes"
  [next-url]
  (let [current-url (get-url)]
    (when (not= next-url current-url)
      (.pushState js/history {}, nil, next-url))))

(defn url-with-params [url params]
  (str url
       (when (seq params) (str "?" (secretary/encode-query-params params)))))

(defn current-url-with-params [params]
  (url-with-params (.-pathname js/location) params))

(defevh :navigate [db page]
  (assoc db :page page))

(defroute "/" []
  (dispatch :navigate :home))

(defroute "/cubes" []
  (dispatch :navigate :cubes))

(defroute "/reports" []
  (dispatch :navigate :reports))

(defroute "/dashboards" []
  (dispatch :navigate :dashboards))

(defroute "/dashboards/new" [query-params]
  (dispatch :dashboards/new query-params))

(defroute "/dashboards/:id" [id query-params]
  (dispatch :dashboards/show id query-params))

(defroute "/profile" []
  (dispatch :navigate :profile/preferences))

(defroute "/profile/password" []
  (dispatch :navigate :profile/password))

(defroute "/configuration" []
  (dispatch :navigate :configuration))

(defroute "/reports/new/:cube" [cube]
  (dispatch :designer/new-report cube))

(defroute "/reports/:id" [id]
  (dispatch :designer/edit-report id))

(defroute "/error" []
  (dispatch :navigate :error))

(defroute "*" []
  (show-page-404))
