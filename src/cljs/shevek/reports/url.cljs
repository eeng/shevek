(ns shevek.reports.url
  (:require [shevek.schemas.conversion :refer [viewer->report]]
            [shevek.lib.util :refer [debounce]]
            [cljs.reader :refer [read-string]]
            [shevek.lib.base64 :as b64]))

(defn store [encoded-report]
  (.replaceState js/history {}, nil, (str "/#/viewer/" (js/encodeURI encoded-report))))

(defn- store-in-url* [viewer]
  (-> viewer viewer->report pr-str b64/encode store))

(def store-in-url (debounce store-in-url* 250))

(defn restore-report-from-url [encoded-report]
  (try
    (-> encoded-report js/decodeURI b64/decode read-string)
    (catch js/Error _ nil)))
