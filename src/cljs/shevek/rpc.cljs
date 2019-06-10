(ns shevek.rpc
  (:require [ajax.core :refer [POST]]
            [shevek.reflow.core :refer [dispatch] :refer-macros [defevh]]
            [shevek.reflow.db :as db]))

(defn loading?
  ([] (seq (db/get :loading)))
  ([key] (db/get-in [:loading key])))

(defn loading [db key]
  (assoc-in db [:loading key] true))

(defn loaded
  ([db] (assoc db :loading {}))
  ([db key] (update db :loading (fnil dissoc {}) key)))

(defn call [fid & {:keys [args handler error-handler]
                   :or {args [] error-handler #(dispatch :errors/from-server %)}}]
  {:pre [(vector? args)]}
  (POST "/rpc" {:params {:fn fid :args args}
                :handler handler
                :error-handler error-handler}))

(defevh :data-arrived [db db-key data db-handler]
  (let [db-handler (or db-handler #(assoc % db-key data))]
    (-> db (loaded db-key) (db-handler data))))

(defn fetch [db db-key fid & {:keys [args handler] :or {args []}}]
  (call fid :args args :handler #(dispatch :data-arrived db-key % handler))
  (loading db db-key))

(defn loading-class [loading-key]
  {:class (when (loading? loading-key) "loading")})
