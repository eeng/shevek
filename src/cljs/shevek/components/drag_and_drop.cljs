(ns shevek.components.drag-and-drop
  (:require [cljs.reader :refer [read-string]]))

(defn drag-start [event transfer-data]
  (.. event -target -classList (add "dragging"))
  (.. event -dataTransfer (setData "application/x-clojure-data" transfer-data)))

(defn drag-end [event]
  (.. event -target -classList (remove "dragging")))

(defn handle-drop [handler]
  (fn [event]
    (let [transfer-data (read-string (.. event -dataTransfer (getData "application/x-clojure-data")))]
      (handler transfer-data)
      (.preventDefault event))))

(defn drag-over [event]
  (.preventDefault event))
