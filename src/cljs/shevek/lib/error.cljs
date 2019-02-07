(ns shevek.lib.error
  (:require [shevek.components.modal :refer [show-modal]]
            [shevek.reflow.core :refer [dispatch] :refer-macros [defevh]]
            [shevek.reflow.db :refer [db]]
            [shevek.i18n :refer [t translation]]
            [shevek.navigation :refer [navigate]]
            [shevek.rpc :as rpc]
            [shevek.lib.logger :as log :refer [debug?]]
            [ajax.core :refer [POST]]
            [clojure.string :as str]))

(defn handle-app-error [{:keys [status status-text response]}]
  (show-modal
   [:div.ui.small.basic.modal
    [:div.ui.icon.red.header
     [:i.warning.circle.icon]
     (str "Error " status ": " status-text)]
    [:div.content response]
    [:div.actions
     [:div.ui.cancel.inverted.button (t :actions/close)]]]))

(defn handle-not-authenticated []
  (dispatch :session-expired))

; TODO DASHBOARD maybe we should display the error page here?
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

(defn report-error-to-server [message stacktrace app-db]
  (POST "/error" {:params {:message message
                           :stacktrace stacktrace
                           :app-db (log/pp-str app-db)} ; Send a string representation instead of the real object because it could be not serializable
                  :headers (rpc/auth-header)}))

(defn uncaught-error-handler [error]
  (-> (js/StackTrace.fromError error)
      (.then
       (fn [stacktrace]
         (report-error-to-server
          (.-message error)
          (->> stacktrace (mapv #(.toString %)) (str/join "\n "))
          (db))))))

(defonce uncaught-error-event-listener
  (do
    (when-not debug?
      (.addEventListener js/window "error" #(uncaught-error-handler (.-error %))))
    :done))

(defevh :errors/unexpected-error [db error]
  (log/error error)
  (when-not debug?
    (js/setTimeout #(uncaught-error-handler error) 500) ; Otherwise StackTrace.JS delays the transition to the error page
    (dispatch :errors/show-page {:message (t :errors/unexpected)})))
