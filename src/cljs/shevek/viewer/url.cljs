(ns shevek.viewer.url
  (:require [shevek.schemas.conversion :refer [viewer->report]]
            [cljs.reader :refer [read-string]]
            [shevek.lib.base64 :as b64]
            [schema.core :as s]
            [shevek.schemas.app-db :refer [CurrentReport]]))

(defn store [encoded-report]
  (let [current-path (.-hash js/location)
        next-path (str "#/viewer/" (js/encodeURI encoded-report))]
    (when (not= next-path current-path)
      (.pushState js/history {}, nil, next-path))))

(defn store-viewer-in-url [{:keys [viewer] :as db}]
  (-> viewer viewer->report pr-str b64/encode store)
  db)

(defn restore-report-from-url [encoded-report]
  (try
    (->> encoded-report js/decodeURI b64/decode read-string (s/validate CurrentReport))
    (catch js/Error _ nil)))
