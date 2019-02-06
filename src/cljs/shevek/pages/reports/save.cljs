(ns shevek.pages.reports.save
  (:require [reagent.core :as r]
            [shevek.rpc :as rpc]
            [shevek.i18n :refer [t]]
            [shevek.reflow.core :refer [dispatch] :refer-macros [defevh]]
            [shevek.components.modal :refer [show-modal close-modal]]
            [shevek.components.form :refer [input-field kb-shortcuts]]
            [shevek.components.notification :refer [notify]]))

(defevh :reports/saved [db {:keys [id name] :as report} after-save]
  (notify (t :reports/saved name))
  (after-save report)
  (close-modal)
  (rpc/loaded db :saving-report))

(defevh :reports/save [db report after-save]
  (rpc/call "reports/save-report" :args [report] :handler #(dispatch :reports/saved % after-save))
  (rpc/loading db :saving-report))

(defn- save-as-dialog [{:keys [report after-save]}]
  (r/with-let [form-data (r/atom (select-keys report [:name :description]))
               valid? #(seq (:name @form-data))
               save #(when (valid?)
                       (dispatch :reports/save (merge report @form-data) after-save))
               shortcuts (kb-shortcuts :enter save)]
    [:<>
     [:div.header (t :actions/save-as)]
     [:div.content
      [:div.ui.form {:ref shortcuts}
       [input-field form-data :name {:label (t :reports/name) :auto-focus true}]
       [input-field form-data :description {:label (t :reports/description) :as :textarea :rows 2}]]]
     [:div.actions
      [:div.ui.green.button
       {:on-click save
        :class [(when-not (valid?) "disabled")
                (when (rpc/loading? :saving-report) "loading disabled")]}
       (t :actions/save)]
      [:div.ui.cancel.button
       (t :actions/cancel)]]]))

(defn open-save-as-dialog [props]
  (show-modal {:modal [save-as-dialog props] :class "tiny"}))
