(ns shevek.settings
  (:require-macros [reflow.macros :refer [defevh]])
  (:require [shevek.i18n :refer [t]]
            [shevek.components :refer [page-title select input-text]]
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
   [:h2.ui.app.header (t :settings/language)]
   [select [["English" "en"] ["Español" "es"]]
    {:selected (db/get-in [:settings :lang] "en")
     :on-change #(dispatch :settings-saved {:lang %})}]])

(defevh :users-arrived [db users]
  (-> (assoc db :users users)
      (rpc/loaded :users)))

(defevh :users-requested [db]
  (rpc/call "users.api/find-all" :handler #(dispatch :users-arrived %))
  (rpc/loading db :users))

(defn- user-row [{:keys [username fullname email]}]
  [:tr
   [:td username]
   [:td fullname]
   [:td email]])

(defn- users-section []
  (dispatch :users-requested)
  (fn []
    [:section
     [:h2.ui.app.header (t :settings/users)]
     [:table.ui.basic.table
      [:thead>tr
       [:th (t :users/username)]
       [:th (t :users/fullname)]
       [:th (t :users/email)]]
      [:tbody
       (rmap user-row (db/get :users) :username)]]]))

(defn- dimension-row [{:keys [name title type]} edited-cube coll-key i]
  [:tr {:key name}
   [:td
    (if @edited-cube
      [:div.ui.fluid.input [input-text edited-cube [coll-key i :title]]]
      title)]
   [:td name]
   [:td type]])

(defn- dimensions-table [original-cube edited-cube header coll-key]
  [:div.dimensions
   [:h4.ui.header header]
   [:table.ui.three.column.basic.table
    [:thead>tr
     [:th (t :cubes.schema/title)]
     [:th (t :cubes.schema/name)]
     [:th (t :cubes.schema/type)]]
    [:tbody
     (for [[i {:keys [name] :as dim}] (map-indexed vector (original-cube coll-key))]
       ^{:key name} [dimension-row dim edited-cube coll-key i])]]])

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
       :class (or (when (= @edited-cube original-cube) "disabled")
                  (when (rpc/loading? :saving-cube) "loading"))}
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
         [input-text edited-cube :title]]
        [:div.ten.wide.field
         [:label (t :cubes.schema/description)]
         [input-text edited-cube :description]]]]
      [:h3.ui.header
       [:i.cube.icon]
       [:div.content title [:div.sub.header description]]])])

(defn- cube-details []
  (let [edited-cube (r/atom nil)]
    (fn [original-cube]
      [:div.cube-details
       [cube-actions original-cube edited-cube]
       [cube-fields original-cube edited-cube]
       [dimensions-table original-cube edited-cube (t :cubes/dimensions) :dimensions]
       [dimensions-table original-cube edited-cube (t :cubes/measures) :measures]])))

(defn- schema-section []
  (dw/fetch-cubes)
  (fn []
    [:section
     [:h2.ui.app.header (t :cubes/menu)]
     (rmap cube-details (dw/cubes-list) :name)]))

(defn page []
 [:div#settings.ui.container
  [page-title (t :settings/title) (t :settings/subtitle) "settings"]
  [user-settings-section]
  [users-section]
  [schema-section]])
