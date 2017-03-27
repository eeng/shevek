(ns shevek.rpc
  (:require-macros [reflow.macros :refer [defevh]])
  (:require [ajax.core :refer [POST]]
            [reflow.core :refer [dispatch]]
            [reflow.db :as db]))

(defn call [fid & {:keys [args handler] :or {args []}}]
  (POST "/rpc" {:params {:fn fid :args args}
                :handler handler}))

(defn loading?
  ([] (seq (db/get :loading)))
  ([key] (db/get-in [:loading key])))

(defn loading [db key]
  (assoc-in db [:loading key] true))

(defn loaded [db key]
  (update db :loading dissoc key))

;; Generic events to make remote queries (doesn't allow to process them before storing in the db)

(defevh :data-requested [db db-key fid & args]
  (call fid :handler #(dispatch :data-arrived db-key %) :args args)
  (loading db db-key))

(defevh :data-arrived [db db-key data]
  (-> (assoc db db-key data)
      (loaded db-key)))

(defn- loading-class [loading-key]
  {:class (when (loading? loading-key) "loading")})
