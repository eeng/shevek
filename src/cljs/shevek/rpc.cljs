(ns shevek.rpc
  (:require [ajax.core :refer [POST]]
            [shevek.reflow.core :refer [dispatch] :refer-macros [defevh]]
            [shevek.reflow.db :as db]
            [shevek.i18n :refer [t]]
            [shevek.lib.local-storage :as local-storage]))

(defn loading?
  ([] (seq (db/get :loading)))
  ([key] (db/get-in [:loading key])))

(defn loading [db key]
  (assoc-in db [:loading key] true))

(defn loaded
  ([db] (assoc db :loading {}))
  ([db key] (update db :loading dissoc key)))

(defn auth-header []
  {"Authorization" (str "Token " (local-storage/get-item "shevek.access-token"))})

(defn call [fid & {:keys [args handler] :or {args []}}]
  {:pre [(vector? args)]}
  (POST "/rpc" {:params {:fn fid :args args}
                :handler handler
                :error-handler #(dispatch :server-error %)
                :headers (auth-header)}))

(defevh :data-arrived [db db-key data db-handler]
  (let [db-handler (or db-handler #(assoc % db-key data))]
    (-> db (loaded db-key) (db-handler data))))

(defn fetch [db db-key fid & {:keys [args handler] :or {args []}}]
  (call fid :args args :handler #(dispatch :data-arrived db-key % handler))
  (loading db db-key))

(defn- loading-class [loading-key]
  {:class (when (loading? loading-key) "loading")})
