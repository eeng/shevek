(ns shevek.lib.time
  (:require #?@(:clj [[clj-time.core :as t]
                      [clj-time.format :as f]]
                :cljs [[cljs-time.core :as t]
                       [cljs-time.format :as f]
                       [cljs-time.coerce :as c]])
            [cuerdas.core :as str]))

(def ^:dynamic *time-zone*
  #?(:clj t/utc
     :cljs (t/default-time-zone)))

(defmacro with-time-zone [tz-id & body]
  `(binding [*time-zone* (t/time-zone-for-id ~tz-id)]
     ~@body))

(defn- from-time-zone [dt]
  #?(:clj (t/from-time-zone dt *time-zone*)
     :cljs (t/from-default-time-zone dt)))

(defn- to-time-zone [dt]
  #?(:clj (t/to-time-zone dt *time-zone*)
     :cljs (t/to-default-time-zone dt)))

(defn system-time-zone []
  (str (t/default-time-zone)))

(defn date-time [& args]
  (from-time-zone (apply t/date-time args)))

(defn parse-time [x]
  (let [converter (if (or (and (string? x) (str/includes? x "T")) (not (string? x)))
                    to-time-zone
                    from-time-zone)]
    #?(:clj
       (cond
         (string? x) (when-let [parsed (f/parse x)]
                       (converter parsed))
         (instance? org.joda.time.DateTime x) x)
       :cljs
       (cond
         (or (string? x) (instance? js/Date x))
         (let [parsed (.parse js/Date x)]
           (when-not (js/isNaN parsed)
             (converter (c/from-long parsed))))
         (instance? goog.date.Date x) x))))

(defn now []
  (to-time-zone (t/now)))

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
  (f/unparse (:date-time f/formatters)
    #?(:clj (t/to-time-zone time t/utc)
       :cljs (t/to-utc-time-zone time))))
