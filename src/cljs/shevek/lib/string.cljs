(ns shevek.lib.string
  (:require [shevek.i18n :refer [t]]
            [clojure.string :as str]))

(def regex-char-esc-smap
  (let [esc-chars "()&^%$#!?*."]
    (zipmap esc-chars
            (map #(str "\\" %) esc-chars))))

(defn regex-escape [string]
  (->> string
       (replace regex-char-esc-smap)
       (apply str)))

(defn format-bool [bool]
  (t (keyword (str "boolean/" bool))))

(defn split [s re]
  (if (seq s)
    (str/split s re)
    []))

(defn present? [s]
  (not (str/blank? s)))
