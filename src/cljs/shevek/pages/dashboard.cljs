(ns shevek.pages.dashboard
  (:require [cljsjs.react-grid-layout]
            [reagent.core :as r]
            [cuerdas.core :as str]
            [com.rpl.specter :refer [transform setval ALL NONE]]
            [shevek.pages.cubes.page :refer [cubes-list]]
            [shevek.pages.designer.page :refer [designer]]
            [shevek.pages.designer.helpers :refer [send-report-query build-new-report get-cube]]
            [shevek.pages.designer.visualization :refer [visualization]]
            [shevek.reflow.core :refer [dispatch] :refer-macros [defevh]]
            [shevek.i18n :refer [t]]
            [shevek.reflow.db :as db]
            [shevek.rpc :as rpc]
            [shevek.lib.collections :refer [detect find-by]]
            [shevek.navigation :refer [current-url-with-params]]
            [shevek.components.notification :refer [notify]]
            [shevek.components.layout :refer [topbar]]))

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
          true (assoc-in [:designer :built?] false)
          (or (not current-dashboard) (not= id (:id current-dashboard))) (init-current-dashboard)))

(defevh :dashboards/new [{:keys [current-dashboard] :as db} query-params]
  (init-page db nil query-params #(assoc % :current-dashboard {:name (t :dashboards/new) :panels []})))

(defevh :dashboards/show [{:keys [current-dashboard] :as db} id query-params]
  (init-page db id query-params #(rpc/fetch % :current-dashboard "dashboards/find-by-id" :args [id] :handler set-panels-ids)))

(defevh :dashboard/new-panel [db]
  (let [new-panel {:report {:name (t :reports/new)}
                   :layout {:x 0 :y 999 :w 8 :h 8}
                   :id (->> (get-in db [:current-dashboard :panels]) (map :id) (apply max 0) inc)}]
    (update-in db [:current-dashboard :panels] conj new-panel)))

(defevh :dashboard/panel-cube-selected [db panel-id cube-name]
  (setval [:current-dashboard :panels ALL (same-id panel-id) :report]
          (build-new-report (get-cube cube-name))
          db))

(defevh :dashboard/delete-panel [db id]
  (-> (setval [:current-dashboard :panels ALL (same-id id)] NONE db)
      (update-in [:current-dashboard :reports-results] dissoc id)))

(defevh :dashboard/layout-changed [db full-layout]
  (let [find-panel-layout (fn [{:keys [id]}]
                            (-> (detect #(= (:i %) (str id)) full-layout)
                                (select-keys [:x :y :w :h])))
        panels (->> (get-in db [:current-dashboard :panels])
                    (mapv (fn [panel] (assoc panel :layout (find-panel-layout panel)))))]
    (assoc-in db [:current-dashboard :panels] panels)))

(defevh :dashboard/save [{:keys [current-dashboard] :as db}]
  (let [dashboard (->> (dissoc current-dashboard :reports-results)
                       (transform [:panels ALL] #(dissoc % :id)))]
    (rpc/call "dashboards/save"
              :args [dashboard]
              :handler #(notify (t :dashboards/saved)))))

(defevh :dashboard/report-changed [db report panel-id]
  (setval [:current-dashboard :panels ALL (same-id panel-id) :report] report db))

(defevh :dashboard/report-query [db report panel-id]
  (send-report-query db report [:current-dashboard :reports-results panel-id]))

(defevh :dashboard/refresh [db]
  (console.log "TODO dashboard refresh")
  db)

(defn- report-visualization [{:keys [report id]}]
  (dispatch :dashboard/report-query report id)
  (fn []
    (if-let [results (db/get-in [:current-dashboard :reports-results id])]
      [visualization results report]
      [:div.ui.active.loader])))

(defn- cube-selector [{:keys [id]}]
  [:div
   (for [{:keys [name title]} (cubes-list)]
     [:div.ui.button {:key name :on-click #(dispatch :dashboard/panel-cube-selected id name)} title])])

(defn- render-panel [{:keys [id]} & _]
  (fn [{{:keys [name cube]} :report :as panel} & [already-fullscreen?]]
    [:div.panel
     [:div.panel-header
      [:div.panel-name name]
      [:div.panel-actions
       (when cube [:a.ui.button {:href (current-url-with-params {:panel id :edit true})} "Edit"])
       (when cube [:a.ui.button {:href (current-url-with-params {:panel id :fullscreen (not already-fullscreen?)})} "Fullscreen"])
       [:a.ui.button {:on-click #(dispatch :dashboard/delete-panel id)} "Remove"]]]
     [:div.panel-content
      (if cube
        [report-visualization panel]
        [cube-selector panel])]]))

(def GridLayout (js/ReactGridLayout.WidthProvider js/ReactGridLayout #js {:measureBeforeMount true}))

(defn- render-panels* [panels animated]
  (let [on-layout-change (fn [layout]
                           (dispatch :dashboard/layout-changed (js->clj layout :keywordize-keys true)))]
    [:> GridLayout {:cols 24
                    :rowHeight 30
                    :className (when animated "animated")
                    :draggableHandle ".panel-header"
                    :draggableCancel ".panel-actions"
                    :onLayoutChange on-layout-change}
     (for [{:keys [id] :as panel} panels]
       [:div {:key id :data-grid (:layout panel)}
        [render-panel panel]])]))

(defn render-panels
  "Removes the initial animation where all panels are positioned according to the layout"
  []
  (let [animated (r/atom false)]
    (r/create-class {:reagent-render (fn [panels] [render-panels* panels @animated])
                     :component-did-mount #(reset! animated true)})))

(defn- dashboard [{:keys [panels]}]
  [:div#dashboard
   [topbar
    :right
    [:<>
     [:button.ui.button
      {:on-click #(dispatch :dashboard/new-panel)}
      (t :dashboards/new-panel)]
     [:button.ui.button
      {:on-click #(dispatch :dashboard/save)}
      (t :dashboards/save)]]]
   [:div.ui.basic.segment
    [render-panels panels]]])

(defn page []
  (when-not (rpc/loading? :current-dashboard) ; TODO DASHBOARD esto produce un glitch en la topbar cada vez q se switchea de dashboard
    (let [{:keys [id edit fullscreen]} (db/get :selected-panel)
          {:keys [report] :as panel} (find-by :id id (db/get-in [:current-dashboard :panels]))]
      (cond ; Could not be a panel when working with a new one, the URL is updated and then the user reload the page
        (and panel edit) [designer report {:on-report-change #(dispatch :dashboard/report-changed % id)}]
        (and panel fullscreen) [:div#dashboard [render-panel panel true]]
        :else [dashboard (db/get :current-dashboard)]))))
