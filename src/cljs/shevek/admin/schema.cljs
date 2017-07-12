(ns shevek.admin.schema
  (:require-macros [reflow.macros :refer [defevh]])
  (:require [shevek.i18n :refer [t]]
            [shevek.components.text :refer [page-title]]
            [shevek.components.form :refer [select text-input input-field kb-shortcuts focused]]
            [shevek.lib.react :refer [rmap]]
            [shevek.lib.dw.cubes :refer [fetch-cubes cubes-list]]
            [shevek.rpc :as rpc]
            [reagent.core :as r]
            [reflow.db :as db]
            [reflow.core :refer [dispatch]]))

(defn- input-or-text [dim edited-cube coll-key i field]
  (if @edited-cube
    [:div.ui.fluid.input [text-input edited-cube [coll-key i field]]]
    [:div (dim field)]))

(defn- dimension-row [{:keys [name type] :as dim} edited-cube coll-key i]
  [:tr {:key name}
   [:td name]
   [:td [input-or-text dim edited-cube coll-key i :title]]
   [:td [input-or-text dim edited-cube coll-key i :description]]
   (if (= coll-key :measures)
     [:td [input-or-text dim edited-cube coll-key i :expression]]
     [:td type])
   (when (= coll-key :measures)
     [:td [input-or-text dim edited-cube coll-key i :format]])])

(defn- dimensions-table [original-cube edited-cube]
  [:div.dimensions
   [:h4.ui.header (t :cubes/dimensions)]
   [:table.ui.basic.table
    [:thead>tr
     [:th.two.wide (t :cubes.schema/name)]
     [:th.three.wide (t :cubes.schema/title)]
     [:th.nine.wide (t :cubes.schema/description)]
     [:th.two.wide (t :cubes.schema/type)]]
    [:tbody
     (for [[i {:keys [name] :as dim}] (map-indexed vector (original-cube :dimensions))]
       ^{:key name} [dimension-row dim edited-cube :dimensions i])]]])

(defn- measures-table [original-cube edited-cube]
  [:div.dimensions
   [:h4.ui.header (t :cubes/measures)]
   [:table.ui.basic.table
    [:thead>tr
     [:th.two.wide (t :cubes.schema/name)]
     [:th.three.wide (t :cubes.schema/title)]
     [:th.five.wide (t :cubes.schema/description)]
     [:th.four.wide (t :cubes.schema/expression)]
     [:th.two.wide (t :cubes.schema/format)]]
    [:tbody
     (for [[i {:keys [name] :as dim}] (map-indexed vector (original-cube :measures))]
       ^{:key name} [dimension-row dim edited-cube :measures i])]]])

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
        cancel #(reset! edited-cube nil)
        shortcuts (kb-shortcuts :enter save :escape cancel)]
    (fn [original-cube]
      [:div.cube-details {:ref shortcuts}
       [cube-actions original-cube edited-cube save cancel]
       [cube-fields original-cube edited-cube]
       [dimensions-table original-cube edited-cube]
       [measures-table original-cube edited-cube (t :cubes/measures)]])))

(defn schema-section []
  (fetch-cubes)
  (fn []
    [:section
     [:h2.ui.app.header (t :cubes/menu)]
     (rmap cube-details (cubes-list) :name)]))
