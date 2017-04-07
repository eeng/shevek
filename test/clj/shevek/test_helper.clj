(ns shevek.test-helper
  (:require [mount.core :as mount]
            [monger.db :refer [drop-db]]
            [shevek.app]
            [shevek.db :refer [db init-db]]
            [clojure.test :refer [deftest testing]]
            [cuerdas.core :as str]))

(defn init []
  (mount/start-without #'shevek.app/nrepl #'shevek.server/web-server))

(defmacro spec [description & body]
  (let [slug (symbol (str/slug description))]
    `(deftest ~slug
       (testing ~description
         (drop-db db)
         (init-db db)
         ~@body))))
