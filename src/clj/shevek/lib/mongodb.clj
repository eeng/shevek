(ns shevek.lib.mongodb
  (:require [monger.joda-time] ; Necessary for automatic joda time objects persistance in monger
            [monger.query :as mq]
            [monger.collection :as mc]
            [monger.operators :refer [$set $setOnInsert]]
            [clj-time.core :refer [now]]
            [com.rpl.specter :refer [transform ALL]])
  (:refer-clojure :exclude [count])
  (:import [org.bson.types ObjectId]))

(defn oid [str]
  (try
    (when str (ObjectId. str))
    (catch java.lang.IllegalArgumentException _)))

(defn timestamp [{:keys [created-at] :as record}]
  (let [updated-at (now)]
    (cond-> (assoc record :updated-at updated-at)
            (not created-at) (assoc :created-at updated-at))))

(defn- foreign-key-simple? [k]
  (re-find #"\-id?$" (name k)))

(defn- foreign-key-multiple? [k]
  (re-find #"\-ids$" (name k)))

(declare wrap-oids)

(defn wrap-oid [[k v]]
  (cond
    (= k :id) [:_id (oid v)]
    (foreign-key-simple? k) [k (oid v)]
    (foreign-key-multiple? k) [k (transform ALL oid v)]
    (and (sequential? v) (map? (first v))) [k (transform ALL wrap-oids v)]
    :else [k v]))

(defn wrap-oids [m]
  (transform ALL wrap-oid m))

(declare unwrap-oids)

(defn- unwrap-oid [[k v]]
  (cond
    (= k :_id) [:id (str v)]
    (foreign-key-simple? k) [k (str v)]
    (foreign-key-multiple? k) [k (transform ALL str v)]
    (and (sequential? v) (map? (first v))) [k (transform ALL unwrap-oids v)]
    :else [k v]))

(defn unwrap-oids [m]
  (transform ALL unwrap-oid m))

(defn find-all [db collection & {:keys [where sort fields] :or {fields []}}]
  (map unwrap-oids
       (mq/with-collection db collection
         (mq/find (wrap-oids where))
         (mq/fields fields)
         (mq/sort sort))))

(defn count [db collection]
  (mc/count db collection))

(defn find-by [db collection condition]
  (unwrap-oids
   (mc/find-one-as-map db collection (wrap-oids condition))))

(defn find-by-id [db collection id]
  (when-let [id (oid id)]
    (unwrap-oids
     (mc/find-map-by-id db collection id))))

(defn throw-record-not-found [collection id]
  (throw (ex-info "Record not found" {:type :shevek/record-not-found :collection collection :id id})))

(defn find-by-id! [db collection id]
  (or (find-by-id db collection id)
      (throw-record-not-found collection id)))

(defn find-last [db collection]
  (last (find-all db collection)))

(defn save [db collection m]
  (->> m
       wrap-oids
       timestamp
       (mc/save-and-return db collection)
       unwrap-oids))

(defn delete-by-id [db collection id]
  (when-let [id (oid id)]
    (mc/remove-by-id db collection id))
  true)

(defn delete-by [db collection condition]
  (mc/remove db collection (wrap-oids condition))
  true)

(defn update-by-id [db collection id fields]
  (mc/update db collection {:_id (oid id)} {$set fields}))

(defn create-or-update-by [db collection condition record]
  (let [{:keys [created-at] :as record} (-> record wrap-oids timestamp)]
    (mc/update db
               collection
               condition
               {$set (dissoc record :created-at)
                $setOnInsert {:created-at created-at}}
               {:upsert true})
    (unwrap-oids (find-by db collection condition))))
