(ns shevek.lib.transit-handlers
  (:require [cognitect.transit :as transit]))

(def object-id-writer
  (transit/write-handler
   (constantly "oid")
   (fn [v] (-> v str))))

; TODO en el client los objectids se ven como TaggedValues xq creo que falta agregar un handler alla tb
#_(let [out (java.io.ByteArrayOutputStream. 2000)]
    (transit/write (transit/writer out :json {:handlers {org.bson.types.ObjectId object-id-writer}})
                 (org.bson.types.ObjectId.))
    (prn out))
