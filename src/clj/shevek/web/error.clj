(ns shevek.web.error
  (:require [shevek.mailer :refer [notify-error]]
            [taoensso.timbre :as log]))

; Queries run within a pmap so if a timeout occours in Druid we will get an ExecutionException with the ExceptionInfo wrapped, hence the root-cause. That way we can translate some errors on the client.
; TODO DASHBOARD antes aca hacia algo mas complejo para poder obtener el Timeout error. Pero el tema es que ante un error de validation schema le llegaba cualquier cosa al client. De todas formas no deberiamos mostrarle el msg original en errores 500 al client. Solo el "oops ocurrio un error inesperado". Quedó el timeout error en las translation, revisar si se lo vamos a seguir mandando en cuyo caso seria mejor atraparlo en capa inferior y meterlo en un error de aplicacion
(defn wrap-server-error [handler]
  (fn [req]
    (try
      (handler req)
      (catch Exception e
        (log/error e)
        (notify-error e {:request (dissoc req :user :optimus-assets)})
        {:status 500 :body (.getMessage e)}))))

(defn client-error [{:keys [params] :as req}]
  (log/error "Client Error:" (:message params) "\n" (:stacktrace params))
  (notify-error (merge params {:request (dissoc req :params :body-params :optimus-assets)}))
  {:status 200 :body "Error Received"})
