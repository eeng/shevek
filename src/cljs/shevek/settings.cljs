(ns shevek.settings
  (:require-macros [reflow.macros :refer [defevh]])
  (:require [shevek.i18n :refer [t]]
            [shevek.components :refer [page-title select]]
            [shevek.lib.local-storage :as local-storage]
            [reflow.db :as db]
            [reflow.core :refer [dispatch]]))

(defn load-settings []
  (dispatch :settings-loaded))

(defn save-settings! [db]
  (local-storage/store! "shevek.settings" (select-keys db [:lang]))
  db)

(defevh :settings-loaded [db]
  (merge db (local-storage/retrieve "shevek.settings")))

(defevh :settings-saved [db settings]
  (-> db (merge settings) save-settings!))

(defn page []
 [:div.ui.container
  [page-title (t :settings/title) (t :settings/subtitle) "settings"]
  [:h2.ui.dividing.header (t :settings/language)]
  [select [["English" "en"] ["Español" "es"]]
   {:selected (db/get :lang "en") :on-change #(dispatch :settings-saved {:lang %})}]
  [:h2.ui.dividing.header (t :settings/users)]
  [:div "TODO Tabla de users"]])
