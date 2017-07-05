(ns shevek.acceptance.test-helper
  (:require [etaoin.api :refer :all]
            [clojure.test :refer [testing]]
            [monger.db :refer [drop-db]]
            [shevek.db :refer [db init-db]]
            [shevek.config :refer [config]]))

; Por defecto etaoin espera 20 segs
(alter-var-root #'etaoin.api/default-timeout (constantly 5))

(defmacro it [description driver & body]
  `(testing ~description
     (with-chrome {} ~driver
       (with-postmortem ~driver {:dir "/tmp"}
         (drop-db db)
         (init-db db)
         ~@body))))

(defn visit [driver path]
  (go driver (str "http://localhost:" (config :port) path)))

(defn- waiting [pred]
  (try
    (wait-predicate pred)
    true
    (catch clojure.lang.ExceptionInfo _
      false)))

(defn has-css? [driver selector attribute value]
  (case attribute
    :text (waiting #(.contains (get-element-text-el driver (query driver {:css selector})) value))
    :count (waiting #(= (count (query-all driver {:css selector})) value))))

(defn has-title? [driver title]
  (has-css? driver "h1.header" :text title))
