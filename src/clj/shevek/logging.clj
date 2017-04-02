(ns shevek.logging
  (:require [taoensso.timbre :as log]))

(defn pp-str [& args]
  (with-out-str (apply clojure.pprint/pprint args)))

(defn- filtered-params [params to-filter]
  (reduce
    #(if (vector? %2)
       (if (get-in %1 %2) (assoc-in %1 %2 "[FILTERED]") %1)
       (if (get %1 %2) (assoc %1 %2 "[FILTERED]") %1))
    params
    to-filter))

(defn user-id [{username :usuario/username}]
  (format "[%s]" (or username "guest")))

(defn wrap-request-logging [handler]
  (fn [{:keys [request-method uri query-string params user] :as req}]
    (let [ps (filtered-params params [:password :password-confirmation])]
      (log/info (user-id user) "Started" request-method uri (str query-string))
      (when (seq ps)
        (log/info (user-id user) :params ps))
      (let [start (System/currentTimeMillis)
            res (handler req)
            total (- (System/currentTimeMillis) start)]
        (log/info (user-id user) "Completed" (:status res) "in" (str total "ms"))
        res))))
