(ns shevek.acceptance.test-helper
  (:require [etaoin.api :as e :refer [wait-predicate wait-visible wait-exists query query-all get-element-text-el go with-chrome with-postmortem exists?]]
            [etaoin.keys :as k]
            [clojure.test :refer [testing]]
            [monger.db :refer [drop-db]]
            [shevek.db :refer [db init-db]]
            [shevek.config :refer [config]]
            [shevek.users.repository :refer [User]]
            [shevek.makers :refer [make!]]))

; Por defecto etaoin espera 20 segs
(alter-var-root #'e/default-timeout (constantly 5))

(defmacro it [description page & body]
  `(testing ~description
     (with-chrome {} ~page
       (with-postmortem ~page {:dir "/tmp"}
         (drop-db db)
         (init-db db)
         ~@body))))

(defmacro pending [& args]
  (let [message (str "\n" name " is pending !!")]
    `(testing ~name (println ~message))))

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
   (waiting #(exists? page {:css selector})))
  ([page selector attribute value]
   (case attribute
     :text (waiting #(.contains (or (element-text page selector) "") value))
     :count (waiting #(= (count (query-all page {:css selector})) value)))))

(defn has-title? [page title]
  (has-css? page "h1.header" :text title))

(defn has-text? [page text]
  (waiting #(e/has-text? page text)))

(defn has-no-text? [page text]
  (waiting #(not (e/has-text? page text))))

(defn click [page q]
  (wait-exists page q)
  (e/click page q))

(defn select [page q option]
  (click page q)
  (click page {:xpath (format "//div[contains(@class, 'active')]//div[contains(@class, 'item') and contains(text(), '%s')]" option)}))

(defn click-link [page text]
  (click page {:xpath (format "//text()[contains(.,'%s')]/ancestor::*[self::a or self::button][1]" text)}))

(defn fill [page field & values]
  (wait-visible page field)
  (apply e/fill page field values))

(defn fill-multi [page fields]
  (wait-visible page (-> fields keys first))
  (e/fill-multi page fields))

(defn login
  ([page] (login page {:username "max" :password "secret123"}))
  ([page {:keys [username password] :as user}]
   (make! User user)
   (when-not (exists? page {:css "#login"})
     (visit page "/"))
   (fill page {:name "username"} username)
   (fill page {:name "password"} password k/enter)
   (has-css? page ".menu")))
