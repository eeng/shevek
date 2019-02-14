(ns shevek.pages.designer.actions.save
  (:require [shevek.i18n :refer [t]]
            [shevek.reflow.core :refer [dispatch] :refer-macros [defevh]]
            [shevek.navigation :refer [set-url]]
            [shevek.components.popup :refer [tooltip]]
            [shevek.pages.reports.save :refer [open-save-as-dialog]]
            [shevek.lib.util :refer [new-record?]]
            [shevek.domain.auth :refer [mine?]]))

(defevh :designer/report-saved [db {:keys [id name] :as report}]
  (set-url (str "/reports/" id))
  (assoc-in db [:designer :report] report))

(defn save-button [report]
  ; Would be not-mine (and have an id) if you enter another user's report via URL
  (let [save-as? (or (new-record? report) (not (mine? report)))]
    [:button.ui.default.icon.button
     {:on-click #(if save-as?
                    (open-save-as-dialog {:report (dissoc report :id)
                                          :after-save (fn [report]
                                                        (dispatch :designer/report-saved report))})
                    (dispatch :reports/save report identity))
      :ref (tooltip (t (if save-as?
                         :actions/save-as
                         :actions/save)))
      :data-tid "save"}
     [:i.save.icon]]))
