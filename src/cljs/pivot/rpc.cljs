(ns pivot.rpc
  (:require-macros [reflow.macros :refer [defevh]])
  (:require [ajax.core :refer [POST]]
            [reflow.core :refer [dispatch]]))

(defn call [fid & {:keys [args handler] :or {args []}}]
  (POST "/rpc" {:params {:fn fid :args args}
                :handler handler}))

(defn loading [db key]
  (update db :loading (fnil conj #{}) key))

(defn loaded [db key]
  (update db :loading disj key))

(defevh :load-data [db db-key fid]
  (call fid :handler #(dispatch :data-arrived db-key %))
  (loading db db-key))

(defevh :data-arrived [db db-key data]
  (-> (assoc db db-key data)
      (loaded db-key)))
