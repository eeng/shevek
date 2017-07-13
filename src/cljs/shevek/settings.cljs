(ns shevek.settings
  (:require-macros [shevek.reflow.macros :refer [defevh]])
  (:require [shevek.reflow.core :refer [dispatch]]
            [shevek.reflow.db :as db]
            [shevek.i18n :refer [t]]
            [shevek.lib.local-storage :as local-storage]
            [shevek.components.form :refer [select]]
            [shevek.components.popup :refer [controlled-popup]]
            [shevek.navegation :refer [current-page]]
            [shevek.schemas.app-db :refer [Settings]]
            [schema-tools.core :as st]
            [cuerdas.core :as str]
            [cljs-time.core :refer [default-time-zone]]))

(defn save-settings! [db]
  (local-storage/store! "shevek.settings" (db :settings))
  db)

(defn refresh-page []
  (let [refresh-events {:dashboard :dashboard/refresh
                        :viewer :viewer/refresh}
        event (refresh-events (current-page))]
    (when event (dispatch event))))

(defonce auto-refresh-interval (atom nil))

(defn set-auto-refresh-interval! [every]
  (js/clearTimeout @auto-refresh-interval)
  (when (and every (pos? every))
    (reset! auto-refresh-interval (js/setInterval refresh-page (* 1000 every)))))

(defn- try-parse [settings]
  (try
    (st/select-schema settings Settings)
    (catch js/Error _ {})))

(def tz-data
  (->> [{:id "America/Argentina/Buenos_Aires" :offset "UTC-03:00" :label "Buenos Aires"}
        {:id "America/Los_Angeles" :offset "UTC-08:00" :label "Los Angeles"}
        {:id "America/New_York" :offset "UTC-05:00" :label "New York"}
        {:id "UTC" :offset "UTC+00:00" :label "UTC"}
        {:id "Europe/Berlin" :offset "UTC+01:00" :label "Berlin"}
        {:id "Africa/Cairo" :offset "UTC+02:00" :label "Cairo"}
        {:id "Asia/Qatar" :offset "UTC+03:00" :label "Qatar"}
        {:id "Asia/Hong_Kong" :offset "UTC+08:00" :label "Hong Kong"}]
       (sort-by :id)))

(defn current-time-zone []
  (or (some #(when (= (:offset %) (:id (default-time-zone))) (:id %)) tz-data)
      "UTC"))

(defevh :settings-loaded [db]
  (let [{:keys [auto-refresh] :as settings} (try-parse (local-storage/retrieve "shevek.settings"))
        settings (merge {:lang "en" :time-zone (current-time-zone)} settings)]
    (set-auto-refresh-interval! auto-refresh)
    (assoc db :settings settings)))

(defevh :settings-saved [db new-settings]
  (-> db (update :settings merge new-settings) save-settings!))

(defn- popup-content []
  [:div#settings-popup.ui.form
   [:h3.ui.sub.orange.header (t :settings/menu)]
   [:div.field
    [:label (t :settings/auto-refresh)]
    [select (t :settings/auto-refresh-opts)
      {:selected (db/get-in [:settings :auto-refresh] 0)
       :on-change #(let [auto-refresh (str/parse-int %)]
                     (set-auto-refresh-interval! auto-refresh)
                     (dispatch :settings-saved {:auto-refresh auto-refresh}))}]
    [:button.ui.fluid.button
     {:on-click #(refresh-page)}
     (t :settings/update-now)]]
   [:div#lang-dropdown.field
    [:label (t :settings/lang)]
    [select [["English" "en"] ["Español" "es"]]
      {:selected (db/get-in [:settings :lang])
       :on-change #(dispatch :settings-saved {:lang %})}]]
   [:div.field
    [:label (t :settings/time-zone)]
    [select (map (juxt :label :id) tz-data)
      {:selected (db/get-in [:settings :time-zone])
       :on-change #(dispatch :settings-saved {:time-zone %})
       :class "search selection"}]]])

(defn- popup-activator [popup]
  [:a.item {:on-click (popup :toggle)}
   [:i.setting.icon] (t :settings/menu)])

(defn- settings-menu []
  [(controlled-popup popup-activator popup-content {:position "bottom right"})])
