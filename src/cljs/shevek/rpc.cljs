(ns shevek.rpc
  (:require-macros [reflow.macros :refer [defevh]])
  (:require [ajax.core :refer [POST]]
            [reflow.core :refer [dispatch]]
            [reflow.db :as db]
            [shevek.i18n :refer [t]]
            [shevek.components.modal :refer [show-modal]]
            [shevek.lib.local-storage :as local-storage]))

(defn loading?
  ([] (seq (db/get :loading)))
  ([key] (db/get-in [:loading key])))

(defn loading [db key]
  (assoc-in db [:loading key] true))

(defn loaded
  ([db] (assoc db :loading {}))
  ([db key] (update db :loading dissoc key)))

(defn handle-not-authenticated []
  (dispatch :session-expired))

(defn handle-app-error [{:keys [status status-text response]}]
  (show-modal {:class "small basic"
               :header [:div.ui.icon.red.header
                        [:i.warning.circle.icon]
                        (str "Error " status ": " status-text)]
               :content response
               :actions [[:div.ui.cancel.inverted.button (t :actions/close)]]}))

(defevh :server-error [db {:keys [status] :as error}]
  (case status
    401 (handle-not-authenticated)
    (handle-app-error error))
  (loaded db))

(defn call [fid & {:keys [args handler] :or {args []}}]
  {:pre [(vector? args)]}
  (POST "/rpc" {:params {:fn fid :args args}
                :handler handler
                :error-handler #(dispatch :server-error %)
                :headers {"Authorization" (str "Token " (local-storage/get-item "shevek.access-token"))}}))

;; Generic events to make remote queries (doesn't allow to process them before storing in the db)

(defevh :data-requested [db db-key fid & args]
  (call fid :handler #(dispatch :data-arrived db-key %) :args args)
  (loading db db-key))

(defevh :data-arrived [db db-key data]
  (-> (assoc db db-key data)
      (loaded db-key)))

(defn- loading-class [loading-key]
  {:class (when (loading? loading-key) "loading")})
