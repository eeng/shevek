(ns shevek.reports
  (:require-macros [reflow.macros :refer [defevh]])
  (:require [reagent.core :as r]
            [reflow.db :as db]
            [reflow.core :refer [dispatch]]
            [shevek.rpc :as rpc]
            [shevek.i18n :refer [t]]
            [shevek.components :refer [controlled-popup kb-shortcuts focused input-field]]
            [shevek.navegation :refer [current-page?]]
            [cuerdas.core :as str]))

; TODO hacer un goog date writer asi no hay que andar haciendo esto
(defn- clean-viewer [viewer]
  (update viewer :cube dissoc :max-time))

(defevh :report-saved [db report]
  (-> (assoc db :current-report report)
      (rpc/loaded :saving-report)))

(defevh :saving-report [db report]
  ; TODO unificar estas dos lineas ya que siempre que hay un call debe haber un loading
  (rpc/call "reports.api/save-report" :args [report (clean-viewer (db :viewer))]
                                      :handler #(dispatch :report-saved %))
  (rpc/loading db :saving-report))

(defn- save-report-form [popup report]
  (let [cancel (popup :close)
        valid? #(seq (:name @report))
        save #(when (valid?) (dispatch :saving-report @report) (cancel))
        shortcuts (kb-shortcuts :enter save :escape cancel)]
    (fn []
      [:div.ui.form {:ref shortcuts}
       [focused input-field report :name {:label (t :reports/name) :class "required"}]
       [input-field report :description {:label (t :reports/description) :as :textarea :rows 2}]
       [input-field report :dashboard {:label (t :reports/dashboard) :as :checkbox :input-class "toggle"}]
       [:button.ui.primary.button {:on-click save :class (when-not (valid?) "disabled")} (t :actions/save)]
       [:button.ui.button {:on-click cancel} (t :actions/cancel)]])))

; TODO Muy parecido a lo de users, de nuevo el patron de call, loading y loaded
(defevh :reports-arrived [db reports]
  (-> (assoc db :reports reports)
      (rpc/loaded :reports)))

(defevh :reports-requested [db report]
  (rpc/call "reports.api/find-all" :handler #(dispatch :reports-arrived %))
  (rpc/loading db :reports))

(defn- reports-list [saving-report current-report]
  (let [reports (db/get :reports)]
    [:div
     (when (current-page? :viewer)
       [:div.actions
        [:button.ui.basic.compact.green.button {:on-click #(reset! saving-report current-report)} (t :actions/save)]
        [:button.ui.basic.compact.button {:on-click #(reset! saving-report (dissoc current-report :_id :name))} (t :actions/save-as)]])
     [:h4.ui.header (t :reports/title)]
     (if (seq reports)
       [:div.ui.relaxed.divided.list
        (for [{:keys [name description]} reports]
          [:div.item {:key name}
           [:a.header name]
           [:div.description (or description (t :cubes/no-desc))]])]
       [:div (t :cubes/no-results)])]))

(defn- popup-content [popup]
  (dispatch :reports-requested)
  (let [saving-report (r/atom nil)
        current-report (or (db/get :current-report) {:dashboard false})]
    (fn []
      [:div#reports-popup
       (if @saving-report
         [save-report-form popup saving-report]
         [reports-list saving-report current-report])])))

(defn- popup-activator [popup]
  (let [report-name (str/prune (db/get-in [:current-report :name]) 30)]
    [:a.item {:on-click (popup :toggle)}
     [:i.line.chart.icon] (or (and (current-page? :viewer) report-name) (t :reports/menu))]))

(defn- reports-menu []
  [(controlled-popup popup-activator popup-content {:position "bottom right"})])
