(ns shevek.lib.error
  (:require-macros [shevek.reflow.macros :refer [defevh]])
  (:require [shevek.components.modal :refer [show-modal]]
            [shevek.reflow.core :refer [dispatch]]
            [shevek.i18n :refer [t translation]]
            [shevek.navegation :refer [navigate]]
            [shevek.rpc :as rpc]
            [shevek.notification :refer [notify]]))

(defn handle-app-error [{:keys [status status-text response]}]
  (show-modal {:class "small basic app-error"
               :header [:div.ui.icon.red.header
                        [:i.warning.circle.icon]
                        (str "Error " status ": " status-text)]
               :content response
               :actions [[:div.ui.cancel.inverted.button (t :actions/close)]]}))

(defn handle-not-authenticated []
  (dispatch :session-expired))

(defn handle-not-authorized [error]
  (handle-app-error error)
  (navigate "/"))

(defevh :server-error [db {:keys [status response] :as error}]
  (case status
    401 (handle-not-authenticated)
    403 (handle-not-authorized (assoc error :response (t :users/unauthorized)))
    502 (handle-app-error (assoc error :response (t :errors/bad-gateway)))
    599 (let [new-response (or (translation :errors (:error response)) (pr-str response))]
          (handle-app-error (assoc error :response new-response :status-text (:error response))))
    (handle-app-error error))
  (rpc/loaded db))

(defevh :client-error [db error]
  (notify error :type :error :timeout 5000)
  (navigate "/")
  (rpc/loaded db))
