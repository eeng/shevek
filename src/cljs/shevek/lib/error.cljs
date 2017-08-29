(ns shevek.lib.error
  (:require-macros [shevek.reflow.macros :refer [defevh]])
  (:require [shevek.components.modal :refer [show-modal]]
            [shevek.reflow.core :refer [dispatch]]
            [shevek.i18n :refer [t translation]]
            [shevek.navegation :refer [navigate]]
            [shevek.rpc :as rpc]))

(defn handle-app-error [{:keys [status status-text response]}]
  (let [error (:error response)
        status-text (or error status-text)
        message (or (and error (translation :errors error)) response)]
    (show-modal {:class "small basic app-error"
                 :header [:div.ui.icon.red.header
                          [:i.warning.circle.icon]
                          (str "Error " status ": " status-text)]
                 :content message
                 :actions [[:div.ui.cancel.inverted.button (t :actions/close)]]})))

(defn handle-not-authenticated []
  (dispatch :session-expired))

(defn handle-not-authorized [error]
  (handle-app-error error)
  (navigate "/"))

(defevh :server-error [db {:keys [status] :as error}]
  (case status
    401 (handle-not-authenticated)
    403 (handle-not-authorized (assoc error :response (t :users/unauthorized)))
    502 (handle-app-error (assoc error :response {:error "Bad Gateway"}))
    (handle-app-error error))
  (rpc/loaded db))
