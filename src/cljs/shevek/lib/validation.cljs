(ns shevek.lib.validation
  (:require [shevek.i18n :refer [t]]
            [clojure.string :refer [blank?]]))

(defn- translate [msg]
  (if (string? msg) msg (t msg)))

(defn pred [field predicate {:keys [msg]}]
  (fn [record]
    (let [value (get record field)]
      (when-not (predicate value)
        [field (translate msg)]))))

(defn required [field]
  (pred field (comp not blank?) {:msg :validation/required}))

(defn- apply-validator [record validator]
  (let [[field msg] (validator record)]
    (if field
      (update-in record [:errors field] (fnil conj []) msg)
      record)))

(defn validate [record validators]
  (reduce apply-validator record validators))

(defn validate! [atom validations]
  (let [{:keys [errors]} (validate @atom validations)]
    (if errors
      (do (swap! atom assoc :errors errors) false)
      (do (swap! atom dissoc :errors) true))))
