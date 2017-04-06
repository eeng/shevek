(ns shevek.settings
  (:require-macros [reflow.macros :refer [defevh]])
  (:require [shevek.i18n :refer [t]]
            [shevek.components :refer [page-title select]]
            [shevek.lib.local-storage :as local-storage]
            [shevek.lib.react :refer [rmap]]
            [shevek.dw :as dw]
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

(defn- user-settings-section []
  [:section
   [:h2.ui.dividing.header (t :settings/language)]
   [select [["English" "en"] ["Español" "es"]]
    {:selected (db/get-in [:settings :lang] "en")
     :on-change #(dispatch :settings-saved {:lang %})}]])

(defn- users-section []
  [:section
   [:h2.ui.dividing.header (t :settings/users)]
   [:div "TODO Tabla de users"]])

(defn- dimension-row [{:keys [name title type]}]
  [:tr {:key name}
   [:td name] [:td title] [:td type]])

(defn- dimensions-table [header dimensions]
  [:div.margin-bottom
   [:h4.ui.header header]
   [:table.ui.three.column.celled.compact.table
    [:thead>tr
     [:th "Name"] [:th "Title"] [:th "Type"]]
    [:tbody
     (map dimension-row dimensions)]]])

(defn- cube-details [{:keys [name title description dimensions measures]}]
  [:div.big.margin-bottom {:key name}
   [:h3.ui.header
    [:i.cube.icon]
    [:div.content title [:div.sub.header description]]]
   [dimensions-table (t :cubes/dimensions) dimensions]
   [dimensions-table (t :cubes/measures) measures]])

(defn- schema-section []
  (dw/fetch-cubes)
  (fn []
    [:section
     [:h2.ui.dividing.header (t :cubes/menu)]
     (rmap cube-details (dw/cubes-list) :name)]))

(defn page []
 [:div#settings.ui.container
  [page-title (t :settings/title) (t :settings/subtitle) "settings"]
  [user-settings-section]
  [users-section]
  [schema-section]])
