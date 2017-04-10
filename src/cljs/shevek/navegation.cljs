(ns shevek.navegation
  (:require-macros [reflow.macros :refer [defevh]])
  (:require [secretary.core :as secretary :refer-macros [defroute]]
            [reflow.core :refer [dispatch]]
            [reflow.db :as db]))

(defevh :navigate [db page]
  (assoc db :page page))

(defroute "/" []
  (dispatch :navigate :dashboard))

(defroute "/settings" []
  (dispatch :navigate :settings))

(defroute "/cubes/:cube" [cube]
  (dispatch :cube-selected cube))

; TODO faltan las rutas para errores

(defn current-page []
  (db/get :page))

(defn current-page? [page]
  (= (current-page) page))
