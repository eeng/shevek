(ns shevek.support.druid
  (:require [clojure.java.io :as io]
            [stub-http.core :refer :all]))

(defn druid-res [name]
  {:status 200
   :content-type "application/json"
   :body (-> (str "druid_responses/" name ".json") io/resource slurp)})

(defn query-req-matching [regex]
  (fn [{:keys [method path body]}]
    (and (= method "POST")
         (= path "/druid/v2")
         (some? (re-find regex (body "postData"))))))

(defmacro with-fake-druid [routes & body]
  `(with-open [server# (start! {:port 4102} ~routes)]
     ~@body))
