(ns shevek.acceptance.test-helper
  (:require [etaoin.api :refer :all]
            [etaoin.keys :as k]
            [clojure.test :refer [testing]]
            [monger.db :refer [drop-db]]
            [shevek.db :refer [db init-db]]
            [shevek.config :refer [config]]
            [shevek.users.repository :refer [User]]
            [shevek.makers :refer [make!]]))

; Por defecto etaoin espera 20 segs
(alter-var-root #'etaoin.api/default-timeout (constantly 5))

(defmacro it [description page & body]
  `(testing ~description
     (with-chrome {} ~page
       (with-postmortem ~page {:dir "/tmp"}
         (drop-db db)
         (init-db db)
         ~@body))))

(defn visit [page path]
  (go page (str "http://localhost:" (config :port) path)))

(defn- waiting [pred]
  (try
    (wait-predicate pred)
    true
    (catch clojure.lang.ExceptionInfo _
      false)))

(defn- element-text [page selector]
  (some->> (query-all page {:css selector}) ; We can't use the query function because it throw error when the element is not found
           first
           (get-element-text-el page)))

(defn has-css?
  ([page selector]
   (wait-visible page {:css selector}))
  ([page selector attribute value]
   (case attribute
     :text (waiting #(.contains (or (element-text page selector) "") value))
     :count (waiting #(= (count (query-all page {:css selector})) value)))))

(defn has-title? [page title]
  (has-css? page "h1.header" :text title))

(defn clickw [page q]
  (wait-exists page q)
  (click page q))

(defn select [page q option]
  (clickw page q)
  (clickw page {:xpath (format "//div[contains(@class, 'active')]//div[contains(@class, 'item') and contains(text(), '%s')]" option)}))

(defn click-link [page text]
  (clickw page {:xpath (format "//text()[contains(.,'%s')]/ancestor::*[self::a or self::button][1]" text)}))

(defn login [page]
  (make! User {:username "someuser" :password "secret123"})
  (visit page "/")
  (fill page {:name "username"} "someuser")
  (fill page {:name "password"} "secret123" k/enter)
  (has-css? page ".menu"))
