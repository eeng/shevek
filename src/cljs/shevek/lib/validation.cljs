(ns shevek.lib.validation
  (:require [shevek.i18n :refer [t]]
            [cuerdas.core :as str]))

(defn- translate [msg]
  (if (string? msg) msg (t msg)))

(defn- apply-validator [record [field validator]]
  (if-let [msg (validator record field)]
    (update-in record [:errors field] (fnil conj []) msg)
    record))

(defn- as-single-field-validator-pairs [validation-map]
  (for [[field validators] validation-map
        validator (flatten (vector validators))]
    [field validator]))

(defn validate [record validation-map]
  (reduce apply-validator record (as-single-field-validator-pairs validation-map)))

(defn validate! [atom validations]
  (swap! atom dissoc :errors)
  (let [{:keys [errors]} (validate @atom validations)]
    (if errors
      (do (swap! atom assoc :errors errors) false)
      true)))

;; Validators

(defn pred [predicate {:keys [msg optional?] :or {optional? true}}]
  (fn [record field]
    (let [value (get record field)
          valid? (or (and optional? (nil? value))
                     (predicate value))]
      (when-not valid?
        (str/format (translate msg) value)))))

(def required (pred (comp seq str/trim str) {:msg :validation/required :optional? false}))

(defn regex [pattern & [opts]]
  (pred (partial re-find pattern) (merge {:msg :validation/regex} opts)))

(def email (regex #"(?i)\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,4}$" {:msg :validation/email}))
