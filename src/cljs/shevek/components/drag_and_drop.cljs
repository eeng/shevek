(ns shevek.components.drag-and-drop
  (:require [cljs.reader :refer [read-string]]))

(defn drag-start [event transfer-data]
  (.. event -dataTransfer (setData "application/x-clojure-data" transfer-data)))

(defn handle-drop [handler]
  (fn [event]
    (let [transfer-data (read-string (.. event -dataTransfer (getData "application/x-clojure-data")))]
      (handler transfer-data)
      (.preventDefault event))))

(defn drag-over [event]
  (.preventDefault event))
