(ns shevek.settings.schema
  (:require-macros [reflow.macros :refer [defevh]])
  (:require [shevek.i18n :refer [t]]
            [shevek.components :refer [page-title select input-text input-field keyboard-shortcuts focused]]
            [shevek.lib.react :refer [rmap]]
            [shevek.dw :as dw]
            [shevek.rpc :as rpc]
            [reagent.core :as r]
            [reflow.db :as db]
            [reflow.core :refer [dispatch]]))

(defn- dimension-row [{:keys [name title type description]} edited-cube coll-key i]
  [:tr {:key name}
   [:td
    (if @edited-cube
      [:div.ui.fluid.input [input-text edited-cube [coll-key i :title]]]
      title)]
   [:td
    (if @edited-cube
      [:div.ui.fluid.input [input-text edited-cube [coll-key i :description]]]
      description)]
   [:td name]
   [:td type]])

(defn- dimensions-table [original-cube edited-cube header coll-key]
  [:div.dimensions
   [:h4.ui.header header]
   [:table.ui.basic.table
    [:thead>tr
     [:th.three.wide (t :cubes.schema/title)]
     [:th.eight.wide (t :cubes.schema/description)]
     [:th.three.wide (t :cubes.schema/name)]
     [:th.two.wide (t :cubes.schema/type)]]
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

(defn- cube-actions [original-cube edited-cube save cancel]
  (if @edited-cube
    [:div.actions
     [:button.ui.primary.button
      {:on-click save
       :class (or (when (= @edited-cube original-cube) "disabled")
                  (when (rpc/loading? :saving-cube) "loading"))}
      (t :actions/save)]
     [:button.ui.button {:on-click cancel} (t :actions/cancel)]]
    [:div.actions
     [:button.ui.button {:on-click #(reset! edited-cube original-cube)} (t :actions/edit)]]))

(defn- cube-fields [{:keys [title description] :as original-cube} edited-cube]
  [:div.cube-fields
    (if @edited-cube
      [:div.ui.form
       [:div.fields
        [focused input-field edited-cube :title {:label (t :cubes.schema/title) :class "six wide"}]
        [input-field edited-cube :description {:label (t :cubes.schema/description) :class "ten wide"}]]]
      [:h3.ui.header
       [:i.cube.icon]
       [:div.content title [:div.sub.header description]]])])

(defn- cube-details []
  (let [edited-cube (r/atom nil)
        save #(dispatch :cube-changed edited-cube)
        cancel #(reset! edited-cube nil)]
    (fn [original-cube]
      [keyboard-shortcuts {:enter save :escape cancel}
       [:div.cube-details
        [cube-actions original-cube edited-cube save cancel]
        [cube-fields original-cube edited-cube]
        [dimensions-table original-cube edited-cube (t :cubes/dimensions) :dimensions]
        [dimensions-table original-cube edited-cube (t :cubes/measures) :measures]]])))

(defn schema-section []
  (dw/fetch-cubes)
  (fn []
    [:section
     [:h2.ui.app.header (t :cubes/menu)]
     (rmap cube-details (dw/cubes-list) :name)]))
