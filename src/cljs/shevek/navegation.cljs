(ns shevek.navegation
  (:require-macros [reflow.macros :refer [defevh]])
  (:require [secretary.core :refer-macros [defroute]]
            [reflow.core :refer [dispatch]]
            [reflow.db :as db]))

(defn current-page []
  (db/get :page))

(defn current-page? [page]
  (= (current-page) page))

(defn navigate [& args]
  (aset js/window "location" (apply str "/#" args)))

(defevh :navigate [db page]
  (assoc db :page page))

(defroute "/" []
  (dispatch :navigate :dashboard))

(defroute "/viewer/:encoded-report" [encoded-report]
  (dispatch :viewer-restored encoded-report))

(defroute "/viewer" []
  (dispatch :navigate :viewer))

(defroute "/admin" []
  (dispatch :navigate :admin))
