(ns shevek.web.logging
  (:require [taoensso.timbre :as log]))

(defn- filtered-params [params to-filter]
  (reduce
    #(if (vector? %2)
       (if (get-in %1 %2) (assoc-in %1 %2 "[FILTERED]") %1)
       (if (get %1 %2) (assoc %1 %2 "[FILTERED]") %1))
    params
    to-filter))

(defn- user-field [{:keys [username]}]
  (format "[%s]" (or username "guest")))

(defn wrap-request-logging [handler]
  (fn [{:keys [request-method uri query-string params identity] :as req}]
    (let [ps (filtered-params params [:password :password-confirmation :current-password])]
      (log/info (user-field identity) "Started" request-method uri (str query-string))
      (when (seq ps)
        (log/info (user-field identity) :params ps))
      (let [start (System/currentTimeMillis)
            res (handler req)
            total (- (System/currentTimeMillis) start)]
        (log/info (user-field identity) "Completed" (:status res) "in" (str total "ms"))
        res))))
