(ns shevek.components.calendar
  (:require [shevek.i18n :refer [translation]]
            [shevek.lib.dates :as dates]
            [cljs-time.coerce :as c]))

(defn- format-js-date [date]
  (if date
    (dates/format-date (c/from-date date))
    ""))

(defn build-range-calendar [dom-node]
  (when dom-node
    (let [from (-> dom-node js/$ (.find ".calendar.from"))
          to (-> dom-node js/$ (.find ".calendar.to"))
          trigger-change #(-> % (.get 0) (.dispatchEvent (js/Event. "input" #js {:bubbles true})))
          shared-opts {:type "date"
                       :today true
                       :text (translation :calendar)
                       :formatter {:date format-js-date}}]
      (.calendar from (clj->js (assoc shared-opts :endCalendar to)))
      (.calendar to (clj->js (assoc shared-opts
                                    :startCalendar from
                                    :onHidden #(do
                                                 (-> from (.find "input") trigger-change)
                                                 (-> to (.find "input") trigger-change))))))))
