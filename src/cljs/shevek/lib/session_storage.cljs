(ns shevek.lib.auth-storage
  (:require [cljs.reader :refer [read-string]]))

(defn set-item! [key val]
  "For storing simple values."
  (.setItem js/sessionStorage key val))

(defn get-item [key]
  (.getItem js/sessionStorage key))

(defn remove-item! [key]
  (.removeItem js/sessionStorage key))

(defn store!
  "For storing maps or other compound values."
  [key value]
  (->> value pr-str (set-item! key)))

(defn retrieve [key]
  (-> key get-item str read-string))
