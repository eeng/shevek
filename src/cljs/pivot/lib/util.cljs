(ns pivot.lib.util)

(defn debounce [f wait]
  (let [timeout (atom nil)]
    (fn [& args]
      (let [later #(do (reset! timeout nil) (apply f args))]
        (when @timeout (js/clearTimeout @timeout))
        (reset! timeout (js/setTimeout later wait))))))
