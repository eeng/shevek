(ns shevek.lib.transit-handlers
  (:require [cognitect.transit :as transit])
  (:import org.bson.types.ObjectId org.joda.time.ReadableInstant org.joda.time.DateTime))

(def object-id-writer
  (transit/write-handler
   (constantly "oid")
   (fn [v] (-> v str))))

(def object-id-reader
  (transit/read-handler
   (fn [v] (-> v ObjectId.))))

(def joda-time-writer
  (transit/write-handler
   (constantly "m")
   (fn [v] (-> ^ReadableInstant v .getMillis))
   (fn [v] (-> ^ReadableInstant v .getMillis .toString))))

(def write-handlers {ObjectId object-id-writer DateTime joda-time-writer})
(def read-handlers {"oid" object-id-reader})

#_(let [out (java.io.ByteArrayOutputStream. 2000)
        oid (org.bson.types.ObjectId.)]
    (prn "original" oid)

    (transit/write (transit/writer out :json {:handlers {ObjectId object-id-writer}}) oid)
    (prn "write" out)

    (def in (java.io.ByteArrayInputStream. (.toByteArray out)))
    (def r (transit/reader in :json {:handlers {"oid" object-id-reader}}))
    (prn "read" (transit/read r)))
