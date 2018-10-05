(ns shevek.lib.error
  (:require [shevek.components.modal :refer [show-modal]]
            [shevek.reflow.core :refer [dispatch] :refer-macros [defevh]]
            [shevek.i18n :refer [t translation]]
            [shevek.navigation :refer [navigate]]
            [shevek.rpc :as rpc]
            [shevek.components.notification :refer [notify]]
            [ajax.core :refer [POST]]))

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

(defevh :server-error [db {:keys [status status-text response] :as error}]
  (case status
    401 (handle-not-authenticated)
    403 (handle-not-authorized (assoc error :response (t :users/unauthorized)))
    502 (handle-app-error (assoc error :response (t :errors/bad-gateway)))
    (let [found-translation (translation :errors response)
          new-response (or found-translation response)
          new-status-text (if found-translation response status-text)]
      (handle-app-error (assoc error :response new-response :status-text new-status-text))))
  (rpc/loaded db))

(defn- uncaught-error-handler [e]
  (POST "/error" {:params {:message (.. e -error -message) :stacktrace (.. e -error -stack)}
                  :headers (rpc/auth-header)}))

(defonce uncaught-error-event-listener
  (do (.addEventListener js/window "error" uncaught-error-handler)
    :done))
