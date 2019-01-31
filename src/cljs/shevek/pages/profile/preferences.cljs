(ns shevek.pages.profile.preferences
  (:require [reagent.core :as r]
            [shevek.reflow.core :refer [dispatch] :refer-macros [defevh defevhi]]
            [shevek.reflow.db :as db]
            [shevek.lib.local-storage :as local-storage]
            [shevek.schemas.app-db :refer [Preferences]]
            [schema-tools.core :as st]
            [shevek.i18n :refer [t]]
            [shevek.components.form :refer [select]]
            [shevek.components.notification :refer [notify]]))

(def default-preferences
  {:lang "en"
   :sidebar-visible true
   :auto-refresh 60
   :abbreviations "default"})

(defn- try-parse [preferences]
  (try
    (st/select-schema preferences Preferences)
    (catch js/Error _ {})))

(defn save-preferences! [{:keys [preferences] :as db}]
  (local-storage/store! "settings" preferences)
  db)

(defevh :preferences/loaded [db]
  (let [preferences (try-parse (local-storage/retrieve "settings"))]
    (assoc db :preferences (merge default-preferences preferences))))

(defevh :preferences/save [db new-preferences]
  (-> db (update :preferences merge new-preferences) save-preferences!))

(defn preferences-panel []
  (r/with-let [pref (r/atom (db/get :preferences))]
    [:div.ui.form.preferences
     [:div.field {:data-tid "lang"}
      [:label (t :preferences/lang)]
      [select [["English" "en"] ["Español" "es"]]
       {:selected (:lang @pref)
        :on-change #(swap! pref assoc :lang %)}]]
     [:div.field
      [:label (t :preferences/abbreviations)]
      [select (t :preferences/abbreviations-opts)
       {:selected (:abbreviations @pref)
        :on-change #(swap! pref assoc :abbreviations %)}]]
     [:button.ui.primary.button
      {:on-click #(do
                    (dispatch :preferences/save @pref)
                    (notify (t :preferences/saved)))}
      (t :actions/save)]]))
