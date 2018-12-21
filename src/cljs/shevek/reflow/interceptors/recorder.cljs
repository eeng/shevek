(ns shevek.reflow.interceptors.recorder)

(defn recorder [interceptor & {:keys [events-to-keep] :or {events-to-keep 20}}]
  (fn [db event]
    (let [recorded-event {:timestamp (js/Date.)
                          :event (first event)}] ; Record only the name of the event for now. If we were to store the whole event with its params we'd have to take care of hiding sensitive data.
      (-> db
          (update :last-events #(->> %1 (cons %2) (take events-to-keep) vec) recorded-event)
          (interceptor event)))))
