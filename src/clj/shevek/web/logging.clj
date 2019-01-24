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

(defn- tag [value]
  (when value
    (format "[%s]" value)))

(defn client-ip [req]
  (if-let [ips (get-in req [:headers "x-forwarded-for"])]
    (-> ips (str/split #",") first)
    (:remote-addr req)))

(defn log-request? [uri]
  (not-any? #(str/starts-with? uri %) ["/public" "/js/out"]))

(defn wrap-request-logging [handler]
  (fn [{:keys [request-method uri query-string params identity uuid] :as req}]
    (let [ps (filtered-params params [:password :password-confirmation :current-password :stacktrace :app-db])
          method (-> request-method name str/upper-case)
          full-uri (str uri (when query-string (str "?" query-string)))
          req-id (subs uuid 0 8)
          user (or (:username identity) "guest")
          prefix (str (tag req-id) " " (tag user))]
      (if (log-request? full-uri)
        (do
          (log/info prefix "Started" method full-uri "for" (client-ip req))
          (when (seq ps)
            (log/info prefix "  Params" ps))
          (let [res (handler req)
                {:keys [status duration]} res]
            (log/info prefix "Completed" status "in" (str duration "ms"))
            res))
        (handler req)))))

(defn wrap-uuid [handler]
  (fn [request]
    (handler (assoc request :uuid (str (java.util.UUID/randomUUID))))))
