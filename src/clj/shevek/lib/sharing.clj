(ns shevek.lib.sharing
  (:require [buddy.core.hash :refer [md5]]
            [buddy.core.codecs :refer [bytes->hex]]))

(defn base-url [request]
  (str (-> request :scheme name)
       "://"
       (get-in request [:headers "host"])))

(defn hash-data [data]
  (-> data pr-str md5 bytes->hex))
