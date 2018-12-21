(ns shevek.reflow.core
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs.core.async :refer [chan put! <!]]
            [shevek.reflow.db :refer [app-db]]
            [shevek.lib.logger :as log]
            [shevek.reflow.router :as router]))

(defonce events (chan))
(defonce coordinator (atom nil))

(defn dispatch [eid & args]
  {:pre [(keyword? eid)]}
  (put! events (into [eid] args)))

(def register-event-handler router/register-event-handler)

(defn- handle-event [event handler app-db]
  (try
    (swap! app-db handler event)
    (catch js/Error e
      (dispatch :errors/unexpected-error e))))

(defn- start-coordinator [app-db handler]
  (go-loop []
    (let [event (<! events)]
      (when (and event (not= event [:shutdown]))
        (handle-event event handler app-db)
        (recur)))))

(defn stop-coordinator []
  (dispatch :shutdown))

(defn init [handler]
  (when @coordinator
    (dispatch :shutdown))
  (log/info "Starting reflow coordinator")
  (reset! coordinator (start-coordinator app-db handler)))
