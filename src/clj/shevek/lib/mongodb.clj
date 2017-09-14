(ns shevek.lib.mongodb
  (:require [monger.joda-time] ; Necessary for automatic joda time objects persistance in monger
            [monger.query :as mq]
            [monger.collection :as mc]
            [clj-time.core :refer [now]]
            [clojure.string :refer [ends-with?]]
            [com.rpl.specter :refer [transform ALL LAST]])
  (:import [org.bson.types ObjectId]))

(defn oid [str]
  (ObjectId. str))

(defn timestamp [{:keys [created-at] :as record}]
  (let [updated-at (now)]
    (cond-> (assoc record :updated-at updated-at)
            (not created-at) (assoc :created-at updated-at))))

(defn- foreign-key? [[k _]]
  (re-find #"\-ids?$" (name k)))

(defn- wrap-oid [x]
  (if (sequential? x)
    (transform ALL oid x)
    (oid x)))

(defn wrap-oids [{:keys [id] :as m}]
  (cond-> (transform [ALL foreign-key? LAST] wrap-oid m)
          id (-> (assoc :_id (oid id))
                 (dissoc :id))))

(defn- unwrap-oid [x]
  (if (sequential? x)
    (transform ALL str x)
    (str x)))

(defn unwrap-oids [{:keys [_id] :as m}]
  (cond-> (transform [ALL foreign-key? LAST] unwrap-oid m)
          _id (-> (assoc :id (str _id))
                  (dissoc :_id))))

(defn find-all [db collection & {:keys [where sort fields] :or {fields []}}]
  (map unwrap-oids
       (mq/with-collection db collection
         (mq/find (wrap-oids where))
         (mq/fields fields)
         (mq/sort sort))))

(defn find-by [db collection condition]
  (unwrap-oids
   (mc/find-one-as-map db collection (wrap-oids condition))))

(defn find-by-id [db collection id]
  (unwrap-oids
   (mc/find-map-by-id db collection (oid id))))

(defn save [db collection m]
  (->> m
       wrap-oids
       timestamp
       (mc/save-and-return db collection)
       unwrap-oids))

(defn delete-by-id [db collection id]
  (mc/remove-by-id db collection (oid id))
  true)

(defn delete-by [db collection condition]
  (mc/remove db collection (wrap-oids condition))
  true)

#_(save shevek.db/db "dashboards" {})
