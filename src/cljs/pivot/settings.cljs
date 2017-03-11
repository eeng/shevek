(ns pivot.settings
  (:require-macros [reflow.macros :refer [defevh]])
  (:require [pivot.i18n :refer [t]]
            [pivot.components :refer [page-title dropdown]]
            [reflow.db :as db]
            [reflow.core :refer [dispatch]]))

(defevh :lang-changed [db lang]
  (assoc db :lang lang))

(defn page []
 [:div.ui.container
  [page-title (t :settings/title) (t :settings/subtitle) "settings"]
  [:h2.ui.dividing.header (t :settings/language)]
  [dropdown [["English" "en"] ["Español" "es"]]
   {:selected (db/get :lang "en") :on-change #(dispatch :lang-changed %)}]
  [:h2.ui.dividing.header (t :settings/users)]
  [:div "TODO Tabla de users"]])
