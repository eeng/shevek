(ns shevek.settings
  (:require-macros [reflow.macros :refer [defevh]])
  (:require [shevek.i18n :refer [t]]
            [shevek.components :refer [page-title select]]
            [shevek.lib.local-storage :as local-storage]
            [shevek.lib.react :refer [rmap]]
            [shevek.dw :as dw]
            [shevek.rpc :as rpc]
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
     [:th (t :cubes.schema/name)]
     [:th (t :cubes.schema/title)]
     [:th (t :cubes.schema/type)]]
    [:tbody
     (map dimension-row dimensions)]]])

(defevh :cube-saved [db {:keys [name] :as cube}]
  (-> (assoc-in db [:cubes name] cube)
      (rpc/loaded :saving-cube)))

(defevh :cube-changed [db edited-cube]
  (rpc/call "schema.api/save-cube" :args [@edited-cube]
            :handler #(do (dispatch :cube-saved %) (reset! edited-cube nil)))
  (rpc/loading db :saving-cube))

(defn- cube-actions [original-cube edited-cube]
  (if @edited-cube
    [:div.actions
     [:button.ui.primary.button
      {:on-click #(dispatch :cube-changed edited-cube)
       :class (when (= @edited-cube original-cube) "disabled")}
      (t :actions/save)]
     [:button.ui.button {:on-click #(reset! edited-cube nil)} (t :actions/cancel)]]
    [:div.actions
     [:button.ui.button {:on-click #(reset! edited-cube original-cube)} (t :actions/edit)]]))

(defn- cube-fields [{:keys [title description] :as original-cube} edited-cube]
  [:div.cube-fields
    (if @edited-cube
      [:div.ui.form
       [:div.fields
        [:div.six.wide.field
         [:label (t :cubes.schema/title)]
         [:input {:type "text" :value (@edited-cube :title)
                  :on-change #(swap! edited-cube assoc :title (.-target.value %))}]]
        [:div.ten.wide.field
         [:label (t :cubes.schema/description)]
         [:input {:type "text" :value (@edited-cube :description)
                  :on-change #(swap! edited-cube assoc :description (.-target.value %))}]]]]
      [:h3.ui.header
       [:i.cube.icon]
       [:div.content title [:div.sub.header description]]])])

(defn- cube-details []
  (let [edited-cube (r/atom nil)]
    (fn [{:keys [dimensions measures] :as original-cube}]
      [:div.cube-details
       [cube-actions original-cube edited-cube]
       [cube-fields original-cube edited-cube]
       [dimensions-table (t :cubes/dimensions) dimensions]
       [dimensions-table (t :cubes/measures) measures]])))

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
