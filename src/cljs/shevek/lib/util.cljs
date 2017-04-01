(ns shevek.lib.util)

(defn debounce [f wait]
  (let [timeout (atom nil)]
    (fn [& args]
      (let [later #(do (reset! timeout nil) (apply f args))]
        (when @timeout (js/clearTimeout @timeout))
        (reset! timeout (js/setTimeout later wait))))))

(defn every [secs f]
  (js/setTimeout #(do (f) (every secs f)) (* 1000 secs)))

(def regex-char-esc-smap
  (let [esc-chars "()&^%$#!?*."]
    (zipmap esc-chars
            (map #(str "\\" %) esc-chars))))

(defn regex-escape [string]
  (->> string
       (replace regex-char-esc-smap)
       (apply str)))

(def debug? ^boolean goog.DEBUG)
