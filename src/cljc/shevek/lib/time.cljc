(ns shevek.lib.time
  (:require #?@(:clj [[clj-time.core :as t]
                      [clj-time.format :as f]]
                :cljs [[cljs-time.core :as t]
                       [cljs-time.format :as f]])))

(def date-time t/date-time)
(def now t/now)
(def days t/days)
(def hours t/hours)
(def minus t/minus)

(defn yesterday []
  (t/minus (now) (days 1)))

(defn beginning-of-day [time]
  (date-time (t/year time) (t/month time) (t/day time) 0 0 0 0))

(defn end-of-day [time]
  (-> (beginning-of-day time)
      (t/plus (t/days 1))
      (t/minus (t/millis 1))))

(defn beginning-of-week [time]
  (let [days-to-bow (-> time t/day-of-week dec)]
    (-> time (t/minus (t/days days-to-bow)) beginning-of-day)))

(defn end-of-week [time]
  (let [days-to-eow (->> time t/day-of-week (- 7))]
    (-> time (t/plus (t/days days-to-eow)) end-of-day)))

(def beginning-of-month (comp beginning-of-day t/first-day-of-the-month))
(def end-of-month (comp end-of-day t/last-day-of-the-month))

(defn ceil [n]
  #?(:clj (Math/ceil n)
     :cljs (Math.ceil n)))

(defn quarter [time]
  (-> time t/month (/ 3) ceil))

(defn beginning-of-quarter [time]
  (date-time (t/year time) (- (* (quarter time) 3) 2)))

(defn end-of-quarter [time]
  (end-of-month (date-time (t/year time) (* (quarter time) 3))))

(defn beginning-of-year [time]
  (date-time (t/year time)))

(defn end-of-year [time]
  (end-of-month (date-time (t/year time) 12)))

(defn round-to-next-minute [time]
  (date-time (t/year time) (t/month time) (t/day time)
             (t/hour time) (mod (inc (t/minute time)) 60) 0))

(defn to-iso8601 [time]
  (f/unparse (:date-time f/formatters) time))

(defn parse-time [str]
  #?(:clj (if (string? str)
            (org.joda.time.DateTime/parse str)
            str)
     :cljs str)) ; TODO revisar, en el server necesito parsear el max-time y los interval xq vienen como strings x ahora, pero en el client ya estaban parseados antes, unificar
