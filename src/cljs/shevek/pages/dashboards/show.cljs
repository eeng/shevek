(ns shevek.pages.dashboards.show
  (:require [cljsjs.react-grid-layout]
            [reagent.core :as r]
            [cuerdas.core :as str]
            [com.rpl.specter :refer [transform setval ALL NONE]]
            [shevek.pages.cubes.list :refer [cubes-list]]
            [shevek.pages.cubes.helpers :refer [cubes-fetcher get-cube]]
            [shevek.pages.designer.page :refer [slave-designer]]
            [shevek.pages.designer.helpers :refer [build-new-report report->query]]
            [shevek.pages.designer.visualization :refer [visualization]]
            [shevek.pages.dashboards.actions.rename :refer [dashboard-name]]
            [shevek.pages.dashboards.actions.save :refer [save-button]]
            [shevek.pages.dashboards.actions.share :refer [share-button]]
            [shevek.pages.dashboards.actions.refresh :refer [refresh-button]]
            [shevek.pages.dashboards.actions.importd :refer [import-button]]
            [shevek.pages.dashboards.helpers :refer [modifiable? master?]]
            [shevek.reflow.core :refer [dispatch] :refer-macros [defevh]]
            [shevek.i18n :refer [t]]
            [shevek.reflow.db :as db]
            [shevek.rpc :as rpc]
            [shevek.lib.collections :refer [detect find-by]]
            [shevek.lib.react :refer [hot-reloading?]]
            [shevek.lib.util :refer [scroll-to]]
            [shevek.navigation :refer [current-url-with-params]]
            [shevek.components.layout :as l :refer [topbar]]
            [shevek.components.popup :refer [tooltip]]
            [shevek.domain.auth :refer [current-user mine?]]))

(def grid-columns 36)
(def first-grid-pos {:x 0 :y 0 :w (/ grid-columns 3) :h 10})

(defn- set-panels-ids [db dashboard]
  (let [update-panels #(for [[id panel] (map-indexed vector %)]
                         (assoc panel :id id))]
    (assoc db :current-dashboard (update dashboard :panels update-panels))))

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

(defn- calculate-new-grid-position [panels]
  (let [w (:w first-grid-pos)
        last-pos (or (-> panels last :grid-pos)
                     {:x 0 :w 0})
        pos {:x (let [x (+ (:x last-pos) (:w last-pos))]
                  (if (> (+ x w) grid-columns) 0 x))
             :y 99}]
    (assoc pos :w w :h (:h first-grid-pos))))

(defn- calculate-new-panel-id [panels]
  (->> panels (map :id) (apply max 0) inc))

(defevh :dashboard/add-panel [{{:keys [panels]} :current-dashboard :as db}]
  (let [new-panel {:type "cube-selector"
                   :grid-pos (calculate-new-grid-position panels)
                   :id (calculate-new-panel-id panels)}]
    (update-in db [:current-dashboard :panels] conj new-panel)))

(defn- same-id [other-id]
  (fn [{:keys [id]}]
    (= id other-id)))

(defevh :dashboard/panel-cube-selected [db panel-id cube-name]
  (transform [:current-dashboard :panels ALL (same-id panel-id)]
             #(assoc % :type "report" :report (build-new-report (get-cube cube-name)))
             db))

(defevh :dashboard/remove-panel [db id]
  (setval [:current-dashboard :panels ALL (same-id id)] NONE db))

(defevh :dashboard/duplicate-panel [{{:keys [panels]} :current-dashboard :as db} panel]
  (let [new-panel (assoc panel
                         :grid-pos (merge (calculate-new-grid-position panels)
                                          (select-keys (:grid-pos panel) [:w :h]))
                         :id (calculate-new-panel-id panels))]
    (js/setTimeout #(scroll-to (str "#panel-" (:id new-panel))) 100)
    (update-in db [:current-dashboard :panels] conj new-panel)))

(defevh :dashboard/layout-changed [db layout]
  (let [find-panel-pos (fn [{:keys [id]}]
                         (-> (detect #(= (:i %) (str id)) layout)
                             (select-keys [:x :y :w :h])))
        panels (->> (get-in db [:current-dashboard :panels])
                    (mapv (fn [panel] (assoc panel :grid-pos (find-panel-pos panel)))))]
    (assoc-in db [:current-dashboard :panels] panels)))

(defevh :dashboard/report-changed [db report panel-id]
  (setval [:current-dashboard :panels ALL (same-id panel-id) :report] report db))

(defevh :dashboard/report-query [db report panel-id state]
  (swap! state assoc :loading? true)
  (rpc/call "querying/query"
            :args [(report->query report)]
            :handler #(reset! state {:loading? false :results %})))

(defn- report-visualization [{:keys [report id]}]
  (let [state (r/atom {:results nil :loading? false})]
    (r/create-class
     {:component-did-mount
      (fn [_]
        (dispatch :dashboard/report-query report id state))

      :component-did-update ; Handles refreshing
      (fn [this [_ prev-props]]
        (when (and (not= (:last-refresh-at prev-props) (:last-refresh-at (r/props this)))
                   (not (:loading? @state))) ; Do not send the refreshing query if the previous is still running
          (dispatch :dashboard/report-query report id state)))

      :reagent-render
      (fn [{:keys [report]}]
        (let [{:keys [results loading?]} @state]
          (if results
            [visualization results report {:refreshing? loading?}]
            [:div.ui.active.loader])))})))

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

(defn- duplicate-panel-button [panel]
  [:a {:on-click #(dispatch :dashboard/duplicate-panel panel)
       :ref (tooltip (t :dashboard/duplicate-panel))}
   [:i.copy.icon]])

(defn- dashboard-panel [{:keys [type report id] :as panel}
                        {:keys [last-refresh-at] :as dashboard}
                        & [{:keys [already-fullscreen?]}]]
  (let [{:keys [name] :or {name (t :dashboard/select-cube)}} report
        modifiable? (modifiable? dashboard)]
    [l/panel
     {:title name
      :id (str "panel-" id) ; For scrolling to
      :actions [[fullscreen-panel-button panel already-fullscreen?]
                (when modifiable? [edit-panel-button panel])
                (when modifiable? [duplicate-panel-button panel])
                (when modifiable? [remove-panel-button panel])]}
     (case type
       "cube-selector" [cube-selector panel]
       "report" [report-visualization (assoc panel :last-refresh-at last-refresh-at)])]))

; Normally panels should have a grid-pos but during testing we usually create them without it.
; Also, the grid-pos was added after some user's dashboards already existed, so this could would have been alternately on a migration
; Lastly, the send to dashboard function takes advantage of this as well, because it doesn't need to set the grid-pos
(defn add-missing-grid-positions [panels]
  (reduce (fn [ready-panels panel]
            (conj ready-panels
                  (assoc panel :grid-pos (calculate-new-grid-position ready-panels))))
          (filterv :grid-pos panels)
          (remove :grid-pos panels)))

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
                    :isResizable (modifiable? dashboard)
                    :margin [15 15]}
     (for [{:keys [id grid-pos] :as panel} (add-missing-grid-positions panels)]
       [:div {:key id :data-grid grid-pos}
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

(defn page* []
  [cubes-fetcher ; The cubes are needed for build the visualization
   (fn []
     (if (rpc/loading? :current-dashboard)
       [topbar] ; Empty placeholder so it doesn't flicker when switching dashboards
       (let [{:keys [id edit fullscreen]} (db/get :selected-panel)
             {:keys [panels] :as dashboard} (db/get :current-dashboard)
             {:keys [report] :as panel} (find-by :id id panels)]
         (cond ; Could not be a panel when working with a new one, the URL is updated and then the user reload the page
           (and panel edit)
           [slave-designer {:report report
                            :on-report-change #(dispatch :dashboard/report-changed % id)}]

           (and panel fullscreen)
           [dashboard-container dashboard
            [:div.fullscreen-panel
             [dashboard-panel panel dashboard {:already-fullscreen? true}]]]

           :else
           [dashboard-container dashboard
            [:div.panels
             [dashboard-panels dashboard]]]))))])

(defevh :dashboard/unmount [db]
  (dissoc db :current-dashboard))

; Clean the dashboard on unmount so every time the user enters a dashboard we start with a clean state.
; The hot-reloading hack is needed because otherwise during dev when this file is reloaded, the will-mount trigger first sending the report queries that depends on the dashboard, which is then cleaned by the will-unmount, so when the results arrive the dashboard is no longer present. Alsa, it is nice to not loose the state during dev
(defn page []
  (r/create-class {:reagent-render page*
                   :component-will-unmount #(when-not (hot-reloading?)
                                              (dispatch :dashboard/unmount))}))
