(ns shevek.pages.designer.actions.save
  (:require [shevek.i18n :refer [t]]
            [shevek.reflow.core :refer [dispatch] :refer-macros [defevh]]
            [shevek.navigation :refer [set-url]]
            [shevek.components.popup :refer [tooltip]]
            [shevek.pages.reports.save :refer [open-save-as-dialog]]))

(defn new-record? [{:keys [id]}]
  (nil? id))

(defevh :designer/report-saved [db {:keys [id name] :as report}]
  (set-url (str "/reports/" id))
  (assoc-in db [:designer :report] report))

(defn save-button [report]
  [:button.ui.icon.default.button
   {:on-click #(if (new-record? report)
                  (open-save-as-dialog {:report report
                                        :after-save (fn [report]
                                                      (dispatch :designer/report-saved report))})
                  (dispatch :reports/save report identity))
    :ref (tooltip (t (if (new-record? report)
                       :actions/save-as
                       :actions/save)))}
   [:i.save.icon]])
