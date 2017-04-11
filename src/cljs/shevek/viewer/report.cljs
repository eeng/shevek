(ns shevek.viewer.report
  (:require-macros [reflow.macros :refer [defevh]])
  (:require [reagent.core :as r]
            [reflow.db :as db]
            [reflow.core :refer [dispatch]]
            [shevek.rpc :as rpc]
            [shevek.i18n :refer [t]]
            [shevek.components :refer [controlled-popup kb-shortcuts focused input-field]]
            [cuerdas.core :as str]))

; TODO hacer un goog date writer asi no hay que andar haciendo esto
(defn- clean-viewer [viewer]
  (update viewer :cube dissoc :max-time))

(defevh :report-saved [db report]
  (-> (assoc db :viewer-report report)
      (rpc/loaded :saving-report)))

(defevh :saving-report [db report]
  ; TODO unificar estas dos lineas ya que siempre que hay un call debe haber un loading
  (rpc/call "reports.api/save-report" :args [report (clean-viewer (db :viewer))]
                                      :handler #(dispatch :report-saved %))
  (rpc/loading db :saving-report))

(defn- save-report-form [popup]
  (let [report (r/atom (or (db/get :viewer-report) {:dashboard false}))
        cancel (popup :close)
        valid? #(seq (:name @report))
        save #(when (valid?) (dispatch :saving-report @report) (cancel))
        shortcuts (kb-shortcuts :enter save :escape cancel)]
    (fn []
      [:div#save-report.ui.form {:ref shortcuts}
       [focused input-field report :name {:label (t :reports/name) :class "required"}]
       [input-field report :description {:label (t :reports/description) :as :textarea :rows 2}]
       [input-field report :dashboard {:label (t :reports/dashboard) :as :checkbox :input-class "toggle"}]
       [:button.ui.primary.button {:on-click save :class (when-not (valid?) "disabled")} (t :actions/save)]
       [:button.ui.button {:on-click cancel} (t :actions/cancel)]])))

(defn- save-report-link [popup]
  (let [report-name (str/prune (db/get-in [:viewer-report :name]) 30)]
    [:a.item {:on-click (popup :toggle)}
     [:i.save.icon] (or report-name (t :reports/menu))]))

(defn- save-report-menu []
  [(controlled-popup save-report-link save-report-form {:position "bottom right"})])
