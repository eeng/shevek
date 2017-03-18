(ns pivot.lib.logging
  (:require [taoensso.timbre :as log]))

; Agrego los milisegundos
(log/merge-config! {:timestamp-opts {:pattern "yy-MM-dd HH:mm:ss.SSS"}})

(defn pp-str [& args]
  (with-out-str (apply clojure.pprint/pprint args)))
