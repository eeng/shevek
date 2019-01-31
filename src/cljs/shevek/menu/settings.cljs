; TODO DASHBOARD vuela
(ns shevek.menu.settings
  (:require [shevek.reflow.core :refer [dispatch] :refer-macros [defevh defevhi]]
            [shevek.reflow.db :as db]
            [shevek.i18n :refer [t]]
            [shevek.rpc :as rpc]
            [shevek.lib.local-storage :as local-storage]
            [shevek.components.form :refer [select]]
            [shevek.components.popup :refer [show-popup tooltip]]
            [shevek.navigation :refer [current-page]]
            [shevek.schemas.app-db :refer [Settings]]
            [shevek.domain.auth :refer [logged-in?]]
            [schema-tools.core :as st]
            [cuerdas.core :as str]))

(defn save-settings! [db]
  (local-storage/store! "settings" (db :settings))
  db)

(defn refresh-page []
  (let [refresh-events {:dashboard :dashboard/refresh
                        :designer :designer/refresh}
        event (refresh-events (current-page))]
    (when (and event (logged-in?)) (dispatch event))))

(defonce auto-refresh-interval (atom nil))

(defn set-auto-refresh-interval! [every]
  (js/clearTimeout @auto-refresh-interval)
  (when (and every (pos? every))
    (reset! auto-refresh-interval (js/setInterval refresh-page (* 1000 every)))))

(defn restart-auto-refresh! []
  (set-auto-refresh-interval! (db/get-in [:preferences :auto-refresh])))

(defn- try-parse [settings]
  (try
    (st/select-schema settings Settings)
    (catch js/Error _ {})))

(def default-settings {:lang "en" :auto-refresh 60 :abbreviations "default"})

(defevh :settings-loaded [db]
  (let [{:keys [auto-refresh] :as settings} (try-parse (local-storage/retrieve "settings"))]
    (set-auto-refresh-interval! auto-refresh)
    (assoc db :settings (merge default-settings settings))))

(defevh :settings-saved [db new-settings]
  (-> db (update :settings merge new-settings) save-settings!))

(defn- popup-content []
  [:div#settings-popup.ui.form
   [:h3.ui.sub.orange.header (t :settings/menu)]
   [:div.field
    [:label (t :settings/auto-refresh)]
    [select (t :settings/auto-refresh-opts)
      {:selected (db/get-in [:preferences :auto-refresh] 0)
       :on-change #(let [auto-refresh (str/parse-int %)]
                     (set-auto-refresh-interval! auto-refresh)
                     (dispatch :settings-saved {:auto-refresh auto-refresh}))}]
    [:button.ui.fluid.button
     (assoc (rpc/loading-class [:designer :report-results])
            :on-click #(refresh-page))
     (t :settings/update-now)]]
   [:div#lang-dropdown.field
    [:label (t :settings/lang)]
    [select [["English" "en"] ["Español" "es"]]
      {:selected (db/get-in [:preferences :lang])
       :on-change #(dispatch :settings-saved {:lang %})}]]
   [:div.field
    [:label (t :settings/abbreviations)]
    [select (t :settings/abbreviations-opts)
      {:selected (db/get-in [:preferences :abbreviations])
       :on-change #(dispatch :settings-saved {:abbreviations %})}]]])

(defn- settings-menu []
  [:a.icon.item {:on-click #(show-popup % popup-content {:position "bottom right"})}
   [:i.setting.icon]])
