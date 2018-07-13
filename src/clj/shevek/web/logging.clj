(ns shevek.web.logging
  (:require [taoensso.timbre :as log]
            [clojure.string :as str]))

(defn- filtered-params [params to-filter]
  (reduce
    #(if (vector? %2)
       (if (get-in %1 %2) (assoc-in %1 %2 "[FILTERED]") %1)
       (if (get %1 %2) (assoc %1 %2 "[FILTERED]") %1))
    params
    to-filter))

(defn- user-field [{:keys [username]}]
  (format "[%s]" (or username "guest")))

(defn client-ip [req]
  (if-let [ips (get-in req [:headers "x-forwarded-for"])]
    (-> ips (str/split #",") first)
    (:remote-addr req)))

(defn- log-request? [uri]
  (not (str/starts-with? uri "/public")))

(defn wrap-request-logging [handler]
  (fn [{:keys [request-method uri query-string params identity] :as req}]
    (let [ps (filtered-params params [:password :password-confirmation :current-password :stacktrace])
          method (-> request-method name str/upper-case)
          full-uri (str uri (when query-string (str "?" query-string)))]
      (if (log-request? full-uri)
        (do
          (log/info (user-field identity) "Started" method full-uri "for" (client-ip req))
          (when (seq ps)
            (log/info (user-field identity) :params ps))
          (let [start (System/currentTimeMillis)
                res (handler req)
                total (- (System/currentTimeMillis) start)]
            (log/info (user-field identity) "Completed" (:status res) "in" (str total "ms"))
            res))
        (handler req)))))
