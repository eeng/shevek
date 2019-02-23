(ns shevek.pages.designer.actions.save
  (:require [shevek.i18n :refer [t]]
            [shevek.rpc :as rpc]
            [shevek.reflow.core :refer [dispatch] :refer-macros [defevh]]
            [shevek.navigation :refer [set-url]]
            [shevek.components.popup :refer [tooltip]]
            [shevek.pages.reports.save :refer [open-save-as-dialog]]
            [shevek.lib.util :refer [new-record?]]
            [shevek.domain.auth :refer [mine?]]))

(defevh :designer/report-saved [db {:keys [id name] :as report}]
  (set-url (str "/reports/" id))
  (assoc-in db [:designer :report] report))

  ; Would be not-mine (and have an id) if you enter another user's report via URL
(defn- owned-and-saved? [report]
  (and (not (new-record? report))
       (mine? report)))

(defn- open-dialog [report]
  (open-save-as-dialog {:report (dissoc report :id)
                        :after-save (fn [report]
                                      (dispatch :designer/report-saved report))}))

(defn save-button [report]
  (let [save? (owned-and-saved? report)]
    [:button.ui.default.icon.button
     {:on-click #(if save?
                    (dispatch :reports/save report identity)
                    (open-dialog report))
      :ref (tooltip (t (if save? :actions/save :actions/save-as)))
      :class (when (rpc/loading? :saving-report) "loading disabled")
      :data-tid "save"}
     [:i.save.icon]]))

(defn save-as-button [report]
  (when (owned-and-saved? report)
    [:button.ui.default.icon.button
     {:on-click #(open-dialog report)
      :ref (tooltip (t :actions/save-as))
      :data-tid "save-as"}
     [:i.copy.icon]]))
