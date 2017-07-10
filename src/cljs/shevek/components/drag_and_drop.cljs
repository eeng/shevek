(ns shevek.components.drag-and-drop
  (:require [cljs.reader :refer [read-string]]))

(defn drag-start [event transfer-data]
  (.. event -dataTransfer (setData "application/x-clojure-data" transfer-data))
  (let [drag-image (-> (js/$ "<div class='drag-image'/>")
                       (.text (.. event -target -innerText))
                       (.appendTo "body")
                       (.get 0))]
    (.. event -dataTransfer (setDragImage drag-image -10 20))))

(defn handle-drop [handler]
  (fn [event]
    (let [transfer-data (read-string (.. event -dataTransfer (getData "application/x-clojure-data")))]
      (handler transfer-data)
      (.preventDefault event)
      (.remove (js/$ ".drag-image")))))

(defn drag-over [event]
  (.preventDefault event))
