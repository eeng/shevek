(ns shevek.lib.transit-handlers
  (:require [cognitect.transit :as transit])
  (:import [org.bson.types ObjectId]))

(def object-id-writer
  (transit/write-handler
   (constantly "oid")
   (fn [v] (-> v str))))

(def object-id-reader
  (transit/read-handler
   (fn [v] (-> v ObjectId.))))

(def write-handlers {ObjectId object-id-writer})
(def read-handlers {"oid" object-id-reader})

#_(let [out (java.io.ByteArrayOutputStream. 2000)
        oid (org.bson.types.ObjectId.)]
    (prn "original" oid)

    (transit/write (transit/writer out :json {:handlers {ObjectId object-id-writer}}) oid)
    (prn "write" out)

    (def in (java.io.ByteArrayInputStream. (.toByteArray out)))
    (def r (transit/reader in :json {:handlers {"oid" object-id-reader}}))
    (prn "read" (transit/read r)))
