(ns shevek.pages.dashboards.actions.save
  (:require [reagent.core :as r]
            [shevek.rpc :as rpc]
            [shevek.i18n :refer [t]]
            [shevek.navigation :refer [set-url]]
            [shevek.reflow.core :refer [dispatch] :refer-macros [defevh]]
            [shevek.components.modal :refer [show-modal close-modal]]
            [shevek.components.form :refer [input-field kb-shortcuts]]
            [shevek.components.notification :refer [notify]]
            [shevek.components.popup :refer [tooltip]]
            [shevek.lib.util :refer [new-record?]]
            [com.rpl.specter :refer [transform setval ALL NONE]]))

(defevh :dashboards/saved [db {:keys [id name] :as dashboard}]
  (set-url (str "/dashboards/" id))
  (notify (t :dashboards/saved))
  (close-modal)
  (-> (update db :current-dashboard merge (dissoc dashboard :panels))
      (rpc/loaded :saving-dashboard)))

(defn- cube-selector? [{:keys [type]}]
  (= type "cube-selector"))

(defevh :dashboards/save [{:keys [current-dashboard] :as db} form-data]
  (let [dashboard (as-> current-dashboard d
                        (merge d form-data)
                        (dissoc d :reports-results)
                        (setval [:panels ALL cube-selector?] NONE d)
                        (transform [:panels ALL] #(dissoc % :id) d))]
    (rpc/call "dashboards/save"
              :args [dashboard]
              :handler #(dispatch :dashboards/saved %))))

(defn- save-as-dialog [{:keys [dashboard]}]
  (r/with-let [form-data (r/atom (select-keys dashboard [:name :description]))
               valid? #(seq (:name @form-data))
               save #(when (valid?)
                       (dispatch :dashboards/save @form-data))
               shortcuts (kb-shortcuts :enter save)]
    [:div.ui.tiny.modal
     [:div.header (t :actions/save-as)]
     [:div.content
      [:div.ui.form {:ref shortcuts}
       [input-field form-data :name {:label (t :dashboards/name) :auto-focus true}]
       [input-field form-data :description {:label (t :dashboards/description) :as :textarea :rows 2}]]]
     [:div.actions
      [:button.ui.green.button
       {:on-click save
        :class [(when-not (valid?) "disabled")
                (when (rpc/loading? :saving-dashboard) "loading disabled")]}
       (t :actions/save)]
      [:button.ui.cancel.button
       (t :actions/cancel)]]]))

(defn save-button [dashboard]
  [:button.ui.default.icon.button
   {:on-click #(if (new-record? dashboard)
                  (show-modal [save-as-dialog {:dashboard dashboard}])
                  (dispatch :dashboards/save dashboard))
    :ref (tooltip (t (if (new-record? dashboard)
                       :actions/save-as
                       :actions/save)))
    :data-tid "save"}
   [:i.save.icon]])
