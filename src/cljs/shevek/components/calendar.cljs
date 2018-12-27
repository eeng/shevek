(ns shevek.components.calendar
  (:require [shevek.i18n :refer [translation]]
            [shevek.lib.time.ext :as t]
            [cljs-time.coerce :as c]))

(defn- format-js-date [date]
  (if date
    (t/format-date (c/from-date date))
    ""))

(defn build-range-calendar [dom-node {:keys [on-range-changed]}]
  (when dom-node
    (let [from (-> dom-node js/$ (.find ".calendar.from"))
          to (-> dom-node js/$ (.find ".calendar.to"))
          shared-opts {:type "date"
                       :today true
                       :text (translation :calendar)
                       :formatter {:date format-js-date}
                       :onHidden #(on-range-changed [(-> from (.find "input") .val) (-> to (.find "input") .val)])}]
      (.calendar from (clj->js (assoc shared-opts :endCalendar to)))
      (.calendar to (clj->js (assoc shared-opts :startCalendar from))))))
