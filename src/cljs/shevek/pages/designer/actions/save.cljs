(ns shevek.pages.designer.actions.save
  (:require [reagent.core :as r]
            [shevek.rpc :as rpc]
            [shevek.i18n :refer [t]]
            [shevek.reflow.core :refer [dispatch] :refer-macros [defevh]]
            [shevek.navigation :refer [set-url]]
            [shevek.components.popup :refer [tooltip]]
            [shevek.components.modal :refer [show-modal close-modal]]
            [shevek.components.form :refer [input-field kb-shortcuts]]
            [shevek.components.notification :refer [notify]]))

(defn new-record? [{:keys [id]}]
  (nil? id))

(defevh :designer/report-saved [db {:keys [id name] :as report}]
  (set-url (str "/reports/" id))
  (notify (t :reports/saved name))
  (close-modal)
  (-> (assoc-in db [:designer :report] report)
      (rpc/loaded :saving-report)))

(defevh :designer/save-report [db report]
  (rpc/call "reports/save-report" :args [report] :handler #(dispatch :designer/report-saved %))
  (rpc/loading db :saving-report))

(defn- save-as-dialog [report]
  (r/with-let [form-data (r/atom (select-keys report [:name :description]))
               valid? #(seq (:name @form-data))
               save #(when (valid?)
                       (dispatch :designer/save-report (merge report @form-data)))
               shortcuts (kb-shortcuts :enter save)]
    [:<>
     [:div.header (t :actions/save-as)]
     [:div.content
      [:div.ui.form {:ref shortcuts}
       [input-field form-data :name {:label (t :reports/name) :class "required" :auto-focus true :on-focus #(-> % .-target .select)}]
       [input-field form-data :description {:label (t :reports/description) :as :textarea :rows 2}]]]
     [:div.actions
      [:div.ui.green.button
       {:on-click save
        :class [(when-not (valid?) "disabled")
                (when (rpc/loading? :saving-report) "loading")]}
       (t :actions/save)]
      [:div.ui.cancel.button
       (t :actions/cancel)]]]))

(defn open-save-as-dialog [report]
  (show-modal {:modal [save-as-dialog report] :class "tiny"}))

(defn save-button [report]
  [:button.ui.icon.default.button
   {:on-click #(if (new-record? report)
                  (open-save-as-dialog report)
                  (dispatch :designer/save-report report))
    :ref (tooltip (t (if (new-record? report)
                       :actions/save-as
                       :actions/save))
                  {:delay 500})}
   [:i.save.icon]])
