(ns shevek.lib.base64)

(defn encode [str]
  (-> str js/encodeURIComponent js/unescape js/btoa))

(defn decode [str]
  (-> str js/atob js/escape js/decodeURIComponent))
