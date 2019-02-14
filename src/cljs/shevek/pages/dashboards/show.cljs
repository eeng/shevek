(ns shevek.pages.dashboards.show
  (:require [cljsjs.react-grid-layout]
            [reagent.core :as r]
            [cuerdas.core :as str]
            [com.rpl.specter :refer [transform setval ALL NONE]]
            [shevek.pages.cubes.list :refer [cubes-list fetch-cubes]]
            [shevek.pages.designer.page :refer [slave-designer]]
            [shevek.pages.designer.helpers :refer [send-report-query build-new-report get-cube]]
            [shevek.pages.designer.visualization :refer [visualization]]
            [shevek.pages.dashboards.actions.rename :refer [dashboard-name]]
            [shevek.pages.dashboards.actions.save :refer [save-button]]
            [shevek.pages.dashboards.actions.share :refer [share-button]]
            [shevek.pages.dashboards.actions.refresh :refer [refresh-button]]
            [shevek.pages.dashboards.actions.importd :refer [import-button]]
            [shevek.pages.dashboards.helpers :refer [modifiable? mine? master?]]
            [shevek.reflow.core :refer [dispatch] :refer-macros [defevh]]
            [shevek.i18n :refer [t]]
            [shevek.reflow.db :as db]
            [shevek.rpc :as rpc]
            [shevek.lib.collections :refer [detect find-by]]
            [shevek.navigation :refer [current-url-with-params]]
            [shevek.components.layout :as l :refer [topbar]]
            [shevek.components.popup :refer [tooltip]]
            [shevek.domain.auth :refer [current-user]]))

(def grid-columns 36)
(def default-grid-pos {:x 0 :y 0 :w (/ grid-columns 3) :h 10})

(defn- set-panels-ids [db dashboard]
  (let [update-panels #(for [[id panel] (map-indexed vector %)]
                         (assoc panel :id id))]
    (assoc db :current-dashboard (update dashboard :panels update-panels))))

(defn- same-id [other-id]
  (fn [{:keys [id]}]
    (= id other-id)))

(defn- init-page [{:keys [current-dashboard] :as db} id {:keys [panel edit fullscreen]} init-current-dashboard]
  (cond-> (assoc db
                 :page :dashboard
                 :selected-panel (when panel {:id (str/parse-int panel)
                                              :edit (str/to-bool edit)
                                              :fullscreen (str/to-bool fullscreen)}))
          (or (not current-dashboard) (not= id (:id current-dashboard))) (init-current-dashboard)))

(defevh :dashboards/new [{:keys [current-dashboard] :as db} query-params]
  (init-page db nil query-params #(assoc % :current-dashboard {:name (t :dashboards/new)
                                                               :panels []
                                                               :owner-id (current-user :id)})))

(defevh :dashboards/show [{:keys [current-dashboard] :as db} id query-params]
  (init-page db id query-params #(rpc/fetch % :current-dashboard "dashboards/find-by-id" :args [id] :handler set-panels-ids)))

(defn- calculate-new-panel-position [panels]
  (let [w (:w default-grid-pos)
        last-pos (or (-> panels last :grid-pos)
                     {:x 0 :w 0})
        pos {:x (let [x (+ (:x last-pos) (:w last-pos))]
                  (if (> (+ x w) grid-columns) 0 x))
             :y 999}]
    (assoc pos :w w :h (:h default-grid-pos))))

(defevh :dashboard/add-panel [{{:keys [panels]} :current-dashboard :as db}]
  (let [new-panel {:type "cube-selector"
                   :grid-pos (calculate-new-panel-position panels)
                   :id (->> panels (map :id) (apply max 0) inc)}]
    (update-in db [:current-dashboard :panels] conj new-panel)))

(defevh :dashboard/panel-cube-selected [db panel-id cube-name]
  (transform [:current-dashboard :panels ALL (same-id panel-id)]
             #(assoc % :type "report" :report (build-new-report (get-cube cube-name)))
             db))

(defevh :dashboard/remove-panel [db id]
  (-> (setval [:current-dashboard :panels ALL (same-id id)] NONE db)
      (update-in [:current-dashboard :reports-results] (fnil dissoc {}) id)))

(defevh :dashboard/layout-changed [db layout]
  (let [find-panel-pos (fn [{:keys [id]}]
                         (-> (detect #(= (:i %) (str id)) layout)
                             (select-keys [:x :y :w :h])))
        panels (->> (get-in db [:current-dashboard :panels])
                    (mapv (fn [panel] (assoc panel :grid-pos (find-panel-pos panel)))))]
    (assoc-in db [:current-dashboard :panels] panels)))

(defevh :dashboard/report-changed [db report panel-id]
  (setval [:current-dashboard :panels ALL (same-id panel-id) :report] report db))

(defevh :dashboard/report-query [db report panel-id]
  (send-report-query db report [:current-dashboard :reports-results panel-id]))

(defn- report-visualization [{:keys [report id]}]
  (dispatch :dashboard/report-query report id)
  (fn []
    (if-let [results (db/get-in [:current-dashboard :reports-results id])]
      [visualization results report {:refreshing? #(rpc/loading? [:current-dashboard :reports-results id])}]
      [:div.ui.active.loader])))

(defn- cube-selector [{:keys [id]}]
  [cubes-list {:on-click #(dispatch :dashboard/panel-cube-selected id %)}])

(defn- edit-panel-button [{:keys [type id]}]
  (when (= type "report")
    [:a {:href (current-url-with-params {:panel id :edit true})
         :ref (tooltip (t :dashboard/edit-panel))
         :data-tid "edit-panel"}
     [:i.pencil.alternate.icon]]))

(defn- fullscreen-panel-button [{:keys [type id]} already-fullscreen?]
  (when (= type "report")
    [:a {:href (current-url-with-params {:panel id :fullscreen (not already-fullscreen?)})
         :ref (tooltip (t :dashboard/fullscreen-panel))}
     [:i.expand.icon]]))

(defn- remove-panel-button [{:keys [id]}]
  [:a {:on-click #(dispatch :dashboard/remove-panel id)
       :ref (tooltip (t :dashboard/remove-panel))}
   [:i.trash.icon]])

(defn- dashboard-panel [{:keys [type report] :as panel} dashboard & [{:keys [already-fullscreen?]}]]
  (let [{:keys [name] :or {name (t :dashboard/select-cube)}} report
        modifiable? (modifiable? dashboard)]
    [l/panel
     {:title name
      :actions [(when modifiable? [edit-panel-button panel])
                [fullscreen-panel-button panel already-fullscreen?]
                (when modifiable? [remove-panel-button panel])]
      :scrollable true}
     (case type
       "cube-selector" [cube-selector panel]
       "report" [report-visualization panel])]))

(def GridLayout (js/ReactGridLayout.WidthProvider js/ReactGridLayout #js {:measureBeforeMount true}))

(defn- dashboard-panels* [{:keys [panels] :as dashboard} animated]
  (let [on-layout-change (fn [layout]
                           (dispatch :dashboard/layout-changed (js->clj layout :keywordize-keys true)))]
    [:> GridLayout {:cols grid-columns
                    :rowHeight 30
                    :className (when animated "animated")
                    :draggableHandle ".panel-header"
                    :draggableCancel ".panel-actions"
                    :onLayoutChange on-layout-change
                    :isDraggable (modifiable? dashboard)
                    :isResizable (modifiable? dashboard)}
     (for [[idx {:keys [id] :as panel}] (map-indexed vector panels)
           ; Normally panels should have a grid-pos but during testing we usually create them without it
           :let [default-grid-pos (assoc default-grid-pos :x (* idx (:w default-grid-pos)))]]
       [:div {:key id :data-grid (get panel :grid-pos default-grid-pos)}
        [dashboard-panel panel dashboard]])]))

(defn dashboard-panels
  "Removes the initial animation where all panels are positioned according to the layout"
  []
  (let [animated (r/atom false)]
    (r/create-class {:reagent-render (fn [dashboard] [dashboard-panels* dashboard @animated])
                     :component-did-mount #(reset! animated true)})))

(defn- add-panel-button []
  [:button.ui.green.labeled.icon.button
   {:on-click #(dispatch :dashboard/add-panel)}
   [:i.plus.icon]
   (t :dashboard/add-panel)])

(defn- dashboard-container [dashboard child]
  [:div#dashboard
   [topbar {:left [dashboard-name dashboard]
            :right (cond
                     (modifiable? dashboard)
                     [:<>
                      [add-panel-button]
                      [:div.divider]
                      [save-button dashboard]
                      [share-button dashboard]
                      [:div.divider]
                      [refresh-button]]

                     (and (master? dashboard) (not (mine? dashboard)))
                     [:<>
                      [import-button dashboard]
                      [:div.divider]
                      [refresh-button]]

                     :else ; slave
                     [refresh-button])}]
   child])

(defn page []
  (fetch-cubes) ; The cubes are needed con build the visualization
  (fn []
    (if (rpc/loading? :current-dashboard)
      [topbar] ; Empty placeholder so it doesn't flicker when switching dashboards
      (let [{:keys [id edit fullscreen]} (db/get :selected-panel)
            {:keys [panels] :as dashboard} (db/get :current-dashboard)
            {:keys [report] :as panel} (find-by :id id panels)]
        (cond ; Could not be a panel when working with a new one, the URL is updated and then the user reload the page
          (and panel edit)
          [slave-designer {:report report
                           :report-results (db/get-in [:current-dashboard :reports-results id])
                           :on-report-change #(dispatch :dashboard/report-changed % id)}]

          (and panel fullscreen)
          [dashboard-container dashboard
           [:div.fullscreen-panel
            [dashboard-panel panel dashboard {:already-fullscreen? true}]]]

          :else
          [dashboard-container dashboard
           [:div.panels
            [dashboard-panels dashboard]]])))))
