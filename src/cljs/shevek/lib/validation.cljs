(ns shevek.lib.validation
  (:require [shevek.i18n :refer [t]]
            [cuerdas.core :as str]))

(defn- translate [msg]
  (if (string? msg) msg (t msg)))

(defn- apply-validator [state [field validator]]
  (if-let [msg (validator state field)]
    (update-in state [:errors field] (fnil conj []) msg)
    state))

(defn- as-single-field-validator-pairs [validation-map]
  (for [[field validators] validation-map
        validator (flatten (vector validators))]
    [field validator]))

(defn validate [state validation-map]
  (reduce apply-validator state (as-single-field-validator-pairs validation-map)))

(defn validate! [atom validations]
  (swap! atom dissoc :errors)
  (let [{:keys [errors]} (validate @atom validations)]
    (if errors
      (do (swap! atom assoc :errors errors) false)
      true)))

;; Validators

(defn state-pred
  "Use this validator if you need to access de field value and the state in the predicate"
  [predicate {:keys [msg optional?] :or {optional? true}}]
  (fn [state field]
    (let [value (get state field)
          valid? (or (and optional? (nil? value))
                     (predicate value state))]
      (when-not valid?
        (str/format (translate msg) value)))))

(defn pred
  "Use this validator if you only need to access the field value in the predicate"
  [predicate opts]
  (state-pred #(predicate %1) opts))

(def required (pred (comp seq str/trim str) {:msg :validation/required :optional? false}))

(defn regex [pattern & [opts]]
  (pred (partial re-find pattern) (merge {:msg :validation/regex} opts)))

(def email (regex #"(?i)\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,4}$" {:msg :validation/email}))

(defn confirmation [other-field & [opts]]
  (state-pred (fn [value state] (= value (get state other-field)))
              (merge {:msg :validation/confirmation :optional? false} opts)))
