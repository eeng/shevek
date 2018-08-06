(ns shevek.web.assets
  (:require [optimus.prime :as optimus]
            [optimus.assets :as assets]
            [optimus.optimizations :as optimizations]
            [optimus.strategies :as strategies]
            [shevek.config :refer [env?]]))

(defn get-assets []
  (concat
    (assets/load-bundle "public"
                        "all.css"
                        ["/css/semantic.min.css" "/css/calendar.min.css" "/css/app.css"])
    (assets/load-bundle "public"
                        "all.js"
                        ["/js/jquery.min.js" "/js/semantic.min.js" "/js/calendar.min.js" "/js/app.js"])))

(defn wrap-asset-pipeline [handler]
  (optimus/wrap handler
                get-assets
                (if (env? :development) optimizations/none optimizations/all)
                (if (env? :development) strategies/serve-live-assets strategies/serve-frozen-assets)))
