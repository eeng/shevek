(ns shevek.lib.error
  (:require [shevek.components.modal :refer [show-modal]]
            [shevek.reflow.core :refer [dispatch] :refer-macros [defevh]]
            [shevek.reflow.db :refer [db]]
            [shevek.i18n :refer [t translation]]
            [shevek.navigation :refer [navigate]]
            [shevek.rpc :as rpc]
            [shevek.lib.transit :refer [transit-request-format]]
            [ajax.core :refer [POST]]
            [clojure.string :as str]))

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

(defn- uncaught-error-handler [event]
  (let [error (.-error event)]
    (-> (js/StackTrace.fromError error)
        (.then
         (fn [stacktrace]
           (POST "/error" {:params {:message (.-message error)
                                    :stacktrace (->> stacktrace
                                                     (mapv #(.toString %))
                                                     (str/join "\n "))
                                    :app-db (db)}
                           :headers (rpc/auth-header)
                           :format transit-request-format}))))))

(defonce uncaught-error-event-listener
  (do
    (.addEventListener js/window "error" uncaught-error-handler)
    :done))
