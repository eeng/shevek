(ns shevek.acceptance.test-helper
  (:require [etaoin.api :as e]
            [etaoin.keys :as k]
            [clojure.test :refer [testing]]
            [mount.core :as mount :refer [defstate]]
            [monger.db :refer [drop-db]]
            [shevek.db :refer [db init-db]]
            [shevek.nrepl :refer [nrepl]]
            [shevek.scheduler :refer [scheduler]]
            [shevek.reloader :refer [reloader]]
            [shevek.config :refer [config]]
            [shevek.schemas.user :refer [User]]
            [shevek.makers :refer [make!]]
            [shevek.users.repository :as users]
            [clojure.string :as str]
            [com.rpl.specter :refer [transform MAP-KEYS ALL]]))

(defstate page
  :start (e/chrome)
  :stop (e/quit page))

; Por defecto etaoin espera 20 segs
(alter-var-root #'e/default-timeout (constantly 3))

(defn start-system [& [{:keys [swap-states]}]]
  (System/setProperty "conf" "test/resources/test-config.edn")
  (-> (mount/except [#'nrepl #'scheduler #'reloader])
      (mount/swap-states swap-states)
      (mount/start)))

(defn stop-system []
  (mount/stop))

(defn wrap-acceptance-tests [f]
  (start-system)
  (f)
  (stop-system))

(defmacro it [description & body]
  `(testing ~description
     (e/with-postmortem page {:dir "/tmp"}
       (drop-db db)
       (init-db db)
       ~@body)))

(defmacro pending [& args]
  (let [message (str "\n" name " is pending !!")]
    `(testing ~name (println ~message))))

(defn visit [path]
  (e/go page (str "http://localhost:" (config :port) path)))

(defn- waiting [pred]
  (try
    (e/wait-predicate pred)
    true
    (catch clojure.lang.ExceptionInfo _
      false)))

(defn wait-visible [q]
  (e/wait-visible page q))

(defn wait-exists [q]
  (e/wait-exists page q))

(defn element-value [q]
  (e/get-element-value page q))

(defn- element-text [selector]
  (->> (e/query-all page {:css selector})
       (map #(e/get-element-text-el page %))
       (str/join "")))

(defn has-css?
  ([selector]
   (waiting #(e/exists? page {:css selector})))
  ([selector attribute value]
   (case attribute
     :text (waiting #(.contains (or (element-text selector) "") value))
     :count (waiting #(= (count (e/query-all page {:css selector})) value)))))

(defn has-title? [title]
  (has-css? "h1.header" :text title))

(defn has-text? [text]
  (waiting #(e/has-text? page text)))

(defn has-no-text? [text]
  (waiting #(not (e/has-text? page text))))

(defn click [q]
  (e/wait-exists page q {:message (str "Element " q " was not found")})
  (e/click page q))

(defn select [q option]
  (click q)
  (click {:xpath (format "//div[contains(@class, 'active')]//div[contains(@class, 'item') and contains(text(), '%s')]" option)}))

(defn click-link [text]
  (click {:xpath (format "//text()[contains(.,'%s')]/ancestor::*[self::a or self::button][1]" text)}))

(defn click-tid [test-id]
  (click {:data-tid test-id}))

(defn fill [field & values]
  (wait-visible field)
  (apply e/fill page field values))

(defn fill-multi [fields]
  (wait-visible  (if (map? fields)
                   (-> fields keys first)
                   (first fields)))
  (e/fill-multi page fields))

(defn fill-by-name [map-of-input-names-to-value]
  (fill-multi
   (transform [MAP-KEYS] (fn [input-name] {:name input-name}) map-of-input-names-to-value)))

(defn fill-active [& args]
  (apply e/fill-active page args))

(defn refresh []
  (e/refresh page))

(defn drag-and-drop [& args]
  (apply e/drag-and-drop page args))

(defn login
  ([] (login {:username "user" :fullname "User" :password "secret666"}))
  ([{:keys [username password] :as user}]
   (when-not (users/find-by-username db username)
     (make! User user))
   (e/js-execute page "try { localStorage.clear() } catch (e) {}") ; Clear session
   (visit "/")
   (has-css? "input[name=username]")
   (e/clear page {:name "username"} {:name "password"})
   (fill {:name "username"} username)
   (fill {:name "password"} password k/enter)
   (has-css? ".menu")))

(defn login-admin []
  (login {:username "adm" :fullname "Admin" :password "secret666" :admin true}))

#_(start-system)
#_(stop-system)
