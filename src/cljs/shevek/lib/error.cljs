(ns shevek.lib.error
  (:require [shevek.components.modal :refer [show-modal]]
            [shevek.reflow.core :refer [dispatch] :refer-macros [defevh]]
            [shevek.reflow.db :refer [db]]
            [shevek.i18n :refer [t translation]]
            [shevek.rpc :as rpc]
            [shevek.lib.logger :as log :refer [debug?]]
            [ajax.core :refer [POST]]
            [clojure.string :as str]))

(defn handle-app-error [{:keys [status status-text response]}]
  (show-modal
   [:div.ui.tiny.basic.error.modal
    [:div.ui.icon.red.header
     [:i.warning.circle.icon]
     (str "Error " status ": " status-text)]
    [:div.content response]
    [:div.actions
     [:div.ui.cancel.inverted.button (t :actions/close)]]]))

(defn handle-not-authenticated []
  (dispatch :sessions/expired))

(defn show-page-404 []
  (dispatch :errors/show-page {:title "Error 404" :message (t :errors/page-not-found)}))

(defevh :errors/from-server [db {:keys [status status-text response] :as error}]
  (case status
    401 (handle-not-authenticated)
    404 (show-page-404)
    502 (handle-app-error (assoc error :response (t :errors/bad-gateway)))
    403 (handle-app-error (assoc error :response (t :users/unauthorized)))
    (handle-app-error (assoc error :response (t :errors/unexpected))))
  (rpc/loaded db))

(defn report-error-to-server [message stacktrace app-db]
  (POST "/error" {:params {:message message
                           :stacktrace stacktrace
                           :app-db (log/pp-str app-db)}})) ; Send a string representation instead of the real object because it could be not serializable

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

(defevh :reflow/unexpected-error [db error]
  (log/error error)
  (when-not debug?
    (js/setTimeout #(uncaught-error-handler error) 500) ; Otherwise StackTrace.JS delays the transition to the error page
    (dispatch :errors/show-page {:message (t :errors/unexpected)})))
