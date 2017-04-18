(ns shevek.settings.menu
  (:require-macros [reflow.macros :refer [defevh]])
  (:require [reflow.core :refer [dispatch]]
            [reflow.db :as db]
            [shevek.i18n :refer [t]]
            [shevek.lib.local-storage :as local-storage]
            [shevek.components :refer [controlled-popup select]]))

(defn load-settings []
  (dispatch :settings-loaded))

(defn save-settings! [db]
  (local-storage/store! "shevek.settings" (db :settings))
  db)

(defevh :settings-loaded [db]
  (assoc db :settings (local-storage/retrieve "shevek.settings")))

(defevh :settings-saved [db new-settings]
  (-> db (update :settings merge new-settings) save-settings!))

(defn- popup-content [{:keys [close]}]
  [:div#settings-popup.ui.form
   [:h3.ui.sub.orange.header (t :settings/menu)]
   [:div.field
    [:label (t :settings/lang)]
    [select [["English" "en"] ["Español" "es"]]
      {:selected (db/get-in [:settings :lang] "en")
       :on-change #(do (dispatch :settings-saved {:lang %}) (close))}]]])

(defn- popup-activator [popup]
  [:a.item {:on-click (popup :toggle)}
   [:i.setting.icon] (t :settings/menu)])

(defn- settings-menu []
  [(controlled-popup popup-activator popup-content {:position "bottom right"})])
