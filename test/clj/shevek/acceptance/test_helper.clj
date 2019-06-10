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

; Set a specific window size so it is the same accross all dev machines, otherwise a click on a far right would be ok on my pc but fail on the CI
(defstate page
  :start (e/chrome {:size [(/ 1920 2) 1050]})
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

(defn has-no-css? [selector]
  (waiting #(not (has-css? selector))))

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

(defn click-text [text]
  (click {:fn/text text}))

(defn click-tid [test-id]
  (click {:data-tid test-id}))

; Not very pretty but the etaoin double click didn't work
(defn double-click [css]
  (e/js-execute page (format "
    var event = new MouseEvent('dblclick', {
      'view': window,
      'bubbles': true,
      'cancelable': true
    });
    document.querySelector('%s').dispatchEvent(event);" css)))

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

(defn- logout []
  (if (e/exists? page {:data-tid "sidebar-logout"})
    (do
      (e/js-execute page "$('.ui.modals.active').remove()")
      (e/click page {:data-tid "sidebar-logout"})
      true)
    false))

(defn logout-if-necessary []
  (when-not (logout)
    (visit "/")
    (e/wait-exists page {:css ".layout"})
    (logout)))

(defn login
  ([] (login {}))
  ([{:keys [username password fullname]
     :or {username "user" password "secret666" fullname "User"}
     :as user}]
   (let [user (merge {:username username :password password :fullname fullname} user)
         user (or (users/find-by-username db username)
                  (make! User user))]
     (logout-if-necessary)
     (has-css? "input[name=username]")
     (e/clear page {:name "username"} {:name "password"})
     (fill {:name "username"} username)
     (fill {:name "password"} password k/enter)
     (has-css? ".menu")
     user)))

(defn login-admin []
  (login {:username "adm" :fullname "Admin" :admin true}))

#_(start-system)
#_(stop-system)
