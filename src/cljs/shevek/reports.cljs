(ns shevek.reports
  (:require-macros [reflow.macros :refer [defevh]])
  (:require [reagent.core :as r]
            [reflow.db :as db]
            [reflow.core :refer [dispatch]]
            [shevek.rpc :as rpc]
            [shevek.i18n :refer [t]]
            [shevek.components :refer [controlled-popup kb-shortcuts focused input-field]]
            [shevek.navegation :refer [current-page? navigate]]
            [cuerdas.core :as str]))

; TODO hacer un goog date writer asi no hay que andar haciendo esto
(defn- clean-viewer [viewer]
  (update viewer :cube dissoc :max-time))

; TODO Muy parecido a lo de users, de nuevo el patron de call, loading y loaded
(defevh :reports-arrived [db reports]
  (-> (assoc db :reports reports)
      (rpc/loaded :reports)))

(defevh :reports-requested [db report]
  (rpc/call "reports.api/find-all" :handler #(dispatch :reports-arrived %))
  (rpc/loading db :reports))

; FIXME Si se recarga la pagina /viewer no va a funcar. Habria que codificar el state en la URL asi si se recarga vuelve a mostrar lo mismo. Luego de hacer eso se podria quitar la route /cubes.
(defevh :report-selected [db {:keys [cube] :as report}]
  (navigate "/viewer")
  (rpc/call "schema.api/cube" :args [cube] :handler #(dispatch :cube-arrived %))
  (-> (assoc db :viewer {:cube {:name cube}} :current-report report)
      (rpc/loading :cube-metadata)))

(defevh :report-saved [db report]
  (-> (assoc db :current-report report)
      (rpc/loaded :save-report)))

(defevh :save-report [db report]
  ; TODO unificar estas dos lineas ya que siempre que hay un call debe haber un loading
  (rpc/call "reports.api/save-report" :args [report (clean-viewer (db :viewer))]
                                      :handler #(dispatch :report-saved %))
  (rpc/loading db :save-report))

(defn- save-report-form [{:keys [close]} report]
  (let [valid? #(seq (:name @report))
        save #(when (valid?) (dispatch :save-report @report) (close))
        shortcuts (kb-shortcuts :enter save :escape close)]
    (fn []
      [:div.ui.form {:ref shortcuts}
       [focused input-field report :name {:label (t :reports/name) :class "required"}]
       [input-field report :description {:label (t :reports/description) :as :textarea :rows 2}]
       [input-field report :dashboard {:label (t :reports/dashboard) :as :checkbox :input-class "toggle"}]
       [:button.ui.primary.button {:on-click save :class (when-not (valid?) "disabled")} (t :actions/save)]
       [:button.ui.button {:on-click close} (t :actions/cancel)]])))

(defn- reports-list [{:keys [close]} editing-report current-report]
  (let [reports (db/get :reports)
        show-actions? (current-page? :viewer)
        save #(if (:_id current-report)
                (do (dispatch :save-report current-report) (close))
                (reset! editing-report current-report))
        save-as #(reset! editing-report (dissoc current-report :_id :name))
        select-report #(do (dispatch :report-selected %) (close))
        cubes (db/get :cubes)]
    [:div
     (when show-actions?
       [:div.actions
        [:button.ui.compact.green.button {:on-click save} (t :actions/save)]
        [:button.ui.basic.compact.button {:on-click save-as} (t :actions/save-as)]])
     [:h3.ui.sub.header {:class (when show-actions? "has-actions")} (t :reports/title)]
     (if (seq reports)
       [:div.ui.relaxed.middle.aligned.selection.list
        (for [{:keys [name description cube] :as report} reports]
          [:div.item {:key name :on-click #(select-report report)}
           [:div.right.floated.content
            [:div.cube (:title (cubes cube))]
            [:div.item-actions
             [:i.pin.icon]
             [:i.edit.icon]
             [:i.trash.icon]]]
           [:div.header name]
           [:div.description (or description (t :cubes/no-desc))]])]
       [:div (t :cubes/no-results)])]))

(defn- popup-content [popup]
  (dispatch :reports-requested)
  (let [editing-report (r/atom nil)
        current-report (or (db/get :current-report) {:dashboard false})]
    (fn []
      [:div#reports-popup
       (if @editing-report
         [save-report-form popup editing-report]
         [reports-list popup editing-report current-report])])))

(defn- popup-activator [popup]
  (let [report-name (str/prune (db/get-in [:current-report :name]) 30)]
    [:a.item {:on-click (popup :toggle)}
     [:i.line.chart.icon] (or (and (current-page? :viewer) report-name) (t :reports/menu))]))

(defn- reports-menu []
  [(controlled-popup popup-activator popup-content {:position "bottom right"})])
