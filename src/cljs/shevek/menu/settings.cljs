(ns shevek.menu.settings
  (:require [shevek.reflow.core :refer [dispatch] :refer-macros [defevh defevhi]]
            [shevek.reflow.db :as db]
            [shevek.i18n :refer [t]]
            [shevek.rpc :as rpc]
            [shevek.lib.local-storage :as local-storage]
            [shevek.components.form :refer [select]]
            [shevek.components.popup :refer [show-popup close-popup tooltip]]
            [shevek.navigation :refer [current-page]]
            [shevek.schemas.app-db :refer [Settings]]
            [shevek.login :refer [logged-in?]]
            [schema-tools.core :as st]
            [cuerdas.core :as str]))

(defn save-settings! [db]
  (local-storage/store! "shevek.menu.settings" (db :settings))
  db)

(defn refresh-page []
  (let [refresh-events {:dashboard :dashboard/refresh
                        :viewer :viewer/refresh}
        event (refresh-events (current-page))]
    (when (and event (logged-in?)) (dispatch event))))

(defonce auto-refresh-interval (atom nil))

(defn set-auto-refresh-interval! [every]
  (js/clearTimeout @auto-refresh-interval)
  (when (and every (pos? every))
    (reset! auto-refresh-interval (js/setInterval refresh-page (* 1000 every)))))

(defn restart-auto-refresh! []
  (set-auto-refresh-interval! (db/get-in [:settings :auto-refresh])))

(defn- try-parse [settings]
  (try
    (st/select-schema settings Settings)
    (catch js/Error _ {})))

(def default-settings {:lang "en" :auto-refresh 60})

(defevh :settings-loaded [db]
  (let [{:keys [auto-refresh] :as settings} (try-parse (local-storage/retrieve "shevek.menu.settings"))]
    (set-auto-refresh-interval! auto-refresh)
    (assoc db :settings (merge default-settings settings))))

(defevhi :settings-saved [db new-settings]
  {:after [close-popup]}
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
     (assoc (rpc/loading-class [:viewer :results :main])
            :on-click #(refresh-page))
     (t :settings/update-now)]]
   [:div#lang-dropdown.field
    [:label (t :settings/lang)]
    [select [["English" "en"] ["Español" "es"]]
      {:selected (db/get-in [:settings :lang])
       :on-change #(dispatch :settings-saved {:lang %})}]]])

(defn- settings-menu []
  [:a.icon.item {:on-click #(show-popup % popup-content {:position "bottom right"})
                 :ref (tooltip (t :settings/menu))}
   [:i.setting.icon]])
