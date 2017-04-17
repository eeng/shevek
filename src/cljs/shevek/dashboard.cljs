(ns shevek.dashboard
  (:require-macros [reflow.macros :refer [defevh]])
  (:require [reflow.core :refer [dispatch]]
            [reflow.db :as db]
            [shevek.i18n :refer [t]]
            [shevek.rpc :as rpc]
            [shevek.components :refer [page-title]]
            [shevek.dw :as dw]
            [shevek.reports.menu :refer [fetch-reports]]
            [shevek.schemas.conversion :refer [report->viewer viewer->query]]
            [shevek.lib.react :refer [rmap]]
            [shevek.viewer.visualization :refer [visualization]]))

(defn- cube-card [{:keys [name title description] :or {description (t :cubes/no-desc)}}]
  [:a.cube.card {:on-click #(dispatch :cube-selected name)}
   [:div.content
    [:div.header [:i.cube.icon] title]
    [:div.meta description]]])

(defn- cubes-cards []
  (dw/fetch-cubes)
  (fn []
    (let [cubes (dw/cubes-list)]
       (if (seq cubes)
         [:div.ui.cards (rmap cube-card cubes :name)]
         [:div.tip [:i.info.circle.icon] (t :cubes/missing)]))))

(defevh :dashboard/query-executed [db results name]
  (-> (assoc-in db [:dashboard name :results :main] results)
      (assoc-in [:dashboard name :arrived-split] (get-in db [:dashboard name :split])) ; TODO mmm no queda muy prolijo esto del arrived-split, lo tengo q hacer aca xq el component visualization lo necesita
      (rpc/loaded [:dashboard name])))

(defevh :dashboard/cube-arrived [db cube {:keys [name] :as report}]
  (let [viewer (report->viewer report (dw/set-cube-defaults cube))
        q (viewer->query (assoc viewer :totals true))]
    (rpc/call "querying.api/query" :args [q] :handler #(dispatch :dashboard/query-executed % name))
    (update-in db [:dashboard name] merge viewer)))

(defevh :dashboard/cube-requested [db {:keys [name cube] :as report}]
  (rpc/call "schema.api/cube" :args [cube] :handler #(dispatch :dashboard/cube-arrived % report))
  (rpc/loading db [:dashboard name]))

; TODO agregar el loading
(defn- report-card [{:keys [name description] :as report}]
  (dispatch :dashboard/cube-requested report) ; TODO esto no hay q hacerlo aca xq sino al actualizar un reporte y volver al dashboard no se reflejan los cambios si el fetch-reports llega luego de renderizar los reportes como estaban antes. Mejor hacerlo en el fetch-reports o sino al guardar un report inmediatamente pisar el db :reports.
  (fn []
    [:a.report.card {:on-click #(dispatch :report-selected report)}
     [:div.content
      [:div.header name]
      [:div.meta description]]
     [:div.content.visualization-container
      [visualization (db/get-in [:dashboard name])]]]))

(defn- reports-cards []
  (fetch-reports)
  (fn []
    (let [reports (filter :pin-in-dashboard (db/get :reports))]
      (if (seq reports)
        [:div.ui.two.stackable.cards (rmap report-card reports :name)]
        [:div.tip [:i.info.circle.icon] (t :reports/none)]))))

(defn page []
  [:div#dashboard.ui.container
   [page-title (t :dashboard/title) (t :dashboard/subtitle) "block layout"]
   [:h2.ui.app.header (t :cubes/title)]
   [cubes-cards]
   [:h2.ui.app.header (t :reports/pinned)]
   [reports-cards]])
