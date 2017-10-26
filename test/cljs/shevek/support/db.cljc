(ns shevek.support.db)

(defmacro with-app-db [db & body]
  `(try
     (reset! shevek.reflow.db/app-db ~db)
     ~@body
     (finally
       (reset! shevek.reflow.db/app-db {}))))
