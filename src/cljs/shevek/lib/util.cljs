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

(defn new-record? [{:keys [_id id]}]
  (and (nil? _id) (nil? id)))
