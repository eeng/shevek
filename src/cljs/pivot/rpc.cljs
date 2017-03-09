(ns pivot.rpc
  (:require [ajax.core :refer [POST]]))

(defn call [fid & {:keys [args handler] :or {args []}}]
  (POST "/rpc" {:params {:fn fid :args args}
                :handler handler}))
