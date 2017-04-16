(ns shevek.reports.url
  (:require [shevek.schemas.conversion :refer [viewer->report]]
            [shevek.lib.util :refer [debounce]]
            [cljs.reader :refer [read-string]]
            [goog.crypt.base64 :as b64]))

(defn store [encoded-report]
  (.replaceState js/history {}, nil, (str "/#/viewer?r=" (js/encodeURI encoded-report))))

; TODO Sin el encoding en base64 la URL era mas corta, pero mas fragil xq se podia editar facilmente y elegir un cubo inexistente y explotaba todo. Investigar algun cifrado o algo que comprima tb a ver si logramos ambas cosas.
(defn- store-in-url* [{:keys [viewer]}]
  (-> viewer viewer->report pr-str b64/encodeString store))

(def store-in-url (debounce store-in-url* 500))

(defn restore-report-from-url [{:keys [r]}]
  (try
    (-> r js/decodeURI b64/decodeString read-string)
    (catch js/Error _ nil)))
