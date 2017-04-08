(ns shevek.settings.regional
  (:require-macros [reflow.macros :refer [defevh]])
  (:require [shevek.i18n :refer [t]]
            [shevek.components :refer [page-title select]]
            [shevek.lib.local-storage :as local-storage]
            [reagent.core :as r]
            [reflow.db :as db]
            [reflow.core :refer [dispatch]]))

(defn load-settings []
  (dispatch :settings-loaded))

(defn save-settings! [db]
  (local-storage/store! "shevek.settings" (db :settings))
  db)

(defevh :settings-loaded [db]
  (assoc db :settings (local-storage/retrieve "shevek.settings")))

(defevh :settings-saved [db new-settings]
  (-> db (update :settings merge new-settings) save-settings!))

(defn regional-section []
  [:section
   [:h2.ui.app.header (t :settings/language)]
   [select [["English" "en"] ["Español" "es"]]
    {:selected (db/get-in [:settings :lang] "en")
     :on-change #(dispatch :settings-saved {:lang %})}]])
