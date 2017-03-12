(ns pivot.rpc
  (:require-macros [reflow.macros :refer [defevh]])
  (:require [ajax.core :refer [POST]]
            [reflow.core :refer [dispatch]]
            [reflow.db :as db]))

(defn call [fid & {:keys [args handler] :or {args []}}]
  (POST "/rpc" {:params {:fn fid :args args}
                :handler handler}))

(defn loading?
  ([] (seq (db/get :loading)))
  ([k] (k (db/get :loading))))

(defn loading [db key]
  (update db :loading (fnil conj #{}) key))

(defn loaded [db key]
  (update db :loading disj key))

(defevh :data-requested [db db-key fid & [{:keys [args post-process] :or {post-process identity}}]]
  (call fid :handler #(dispatch :data-arrived db-key post-process %) :args args)
  (loading db db-key))

(defevh :data-arrived [db db-key post-process data]
  (-> (assoc db db-key (post-process data))
      (loaded db-key)))
