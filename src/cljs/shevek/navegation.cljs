(ns shevek.navegation
  (:require-macros [reflow.macros :refer [defevh]])
  (:require [secretary.core :as secretary :refer-macros [defroute]]
            [reflow.core :refer [dispatch]]
            [reflow.db :as db]))

(defevh :navigate [db page]
  (assoc db :page page))

(defroute "/" []
  (dispatch :navigate :dashboard))

(defroute "/viewer/:encoded-report" [encoded-report]
  (dispatch :viewer-restored encoded-report))

(defroute "/viewer" []
  (dispatch :navigate :viewer))

(defroute "/settings" []
  (dispatch :navigate :settings))

(defn current-page []
  (db/get :page))

(defn current-page? [page]
  (= (current-page) page))

(defn navigate [& args]
  (aset js/window "location" (apply str "/#" args)))
