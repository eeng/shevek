(ns shevek.pages.dashboards.actions.importd
  (:require [reagent.core :as r]
            [shevek.rpc :as rpc]
            [shevek.reflow.core :refer [dispatch] :refer-macros [defevh]]
            [shevek.i18n :refer [t]]
            [shevek.components.modal :refer [show-modal close-modal]]
            [shevek.components.form :refer [input-field kb-shortcuts]]
            [shevek.components.notification :refer [notify]]
            [shevek.navigation :refer [navigate]]))

(defevh :dashboards/import [db {:keys [id]} form-data]
  (rpc/call "dashboards/import"
            :args [(assoc form-data :master-id id)]
            :handler (fn [new-dash]
                        (navigate (str "/dashboards/" (:id new-dash)))
                        (notify (t :dashboard/imported))))
  (close-modal)
  db)

(defn- import-dialog [dashboard]
  (r/with-let [form-data (r/atom (select-keys dashboard [:name :description]))
               valid? #(seq (:name @form-data))
               save #(when (valid?)
                       (dispatch :dashboards/import dashboard @form-data))
               shortcuts (kb-shortcuts :enter save)]
    [:div.ui.tiny.modal
     [:div.header (t :dashboard/import)]
     [:div.content
      [:p (t :dashboard/import-desc)]
      [:div.ui.form {:ref shortcuts}
       [input-field form-data :name {:label (t :dashboard/import-name)
                                     :auto-focus true
                                     :on-focus #(-> % .-target .select)}]]]
     [:div.actions
      [:button.ui.positive.button
       {:on-click save
        :class (when-not (valid?) "disabled")}
       (t :actions/import)]
      [:button.ui.cancel.button
       (t :actions/cancel)]]]))

(defn import-button [dashboard]
  [:button.ui.green.labeled.icon.button
   {:on-click #(show-modal [import-dialog dashboard])}
   [:i.cloud.download.icon]
   (t :dashboard/import)])
