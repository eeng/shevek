(ns shevek.viewer.url
  (:require [shevek.schemas.conversion :refer [viewer->report]]
            [cljs.reader :refer [read-string]]
            [shevek.lib.base64 :as b64]
            [schema.core :as s]
            [shevek.schemas.app-db :refer [CurrentReport]]
            [shevek.lib.logger :as log]
            [shevek.navigation :refer [set-url]]))

(defn store [encoded-report]
  (set-url (str "/viewer/" (js/encodeURI encoded-report))))

(defn store-viewer-in-url [{:keys [viewer] :as db}]
  (-> viewer viewer->report pr-str b64/encode store)
  db)

(defn restore-report-from-url [encoded-report]
  (try
    (->> encoded-report js/decodeURI b64/decode read-string (s/validate CurrentReport))
    (catch js/Error e
      (log/error e)
      nil)))
