(ns shevek.lib.util)

(defn variable-debounce
  "Allows to debounce a function with a different timeout on each call"
  [f]
  (let [timeout (atom nil)]
    (fn [& wait args]
      (let [later #(do (reset! timeout nil) (apply f args))]
        (when @timeout (js/clearTimeout @timeout))
        (reset! timeout (js/setTimeout later wait))))))

(defn debounce
  "Calls f after 'wait' milliseconds, but if another call is made before that timeout it will be recreate the timeout"
  [f wait]
  (partial (variable-debounce f) wait))

(defn every [secs f]
  (js/setInterval f (* 1000 secs)))

(def regex-char-esc-smap
  (let [esc-chars "()&^%$#!?*."]
    (zipmap esc-chars
            (map #(str "\\" %) esc-chars))))

(defn regex-escape [string]
  (->> string
       (replace regex-char-esc-smap)
       (apply str)))
