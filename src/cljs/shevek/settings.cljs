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
  (local-storage/store! "shevek.settings" (db :settings))
  db)

(defevh :settings-loaded [db]
  (assoc db :settings (local-storage/retrieve "shevek.settings")))

(defevh :settings-saved [db new-settings]
  (-> db (update :settings merge new-settings) save-settings!))

(defn page []
 [:div.ui.container
  [page-title (t :settings/title) (t :settings/subtitle) "settings"]
  [:h2.ui.dividing.header (t :settings/language)]
  [select [["English" "en"] ["Español" "es"]]
   {:selected (db/get-in [:settings :lang] "en") :on-change #(dispatch :settings-saved {:lang %})}]
  [:h2.ui.dividing.header (t :settings/users)]
  [:div "TODO Tabla de users"]])
