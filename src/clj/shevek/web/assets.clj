(ns shevek.web.assets
  "This guy is in charge of concatenating, minimizing (although we don't needed it) and fingerprinting assets."
  (:require [optimus.prime :as optimus]
            [optimus.assets :as assets]
            [optimus.optimizations :as optimizations]
            [optimus.strategies :as strategies]
            [shevek.config :refer [env?]]
            [com.rpl.specter :refer [transform select-one! ALL]]))

(defn- fix-source-map-path
  [assets]
  "Without this, the app.js.map file would appear with a different cache buster hash than the corresponding app.js. They must have the same for StackTrace.JS to found it"
  (let [js-path (select-one! [ALL :path #(re-matches #"/bundles/\w+/app.js" %)] assets)
        source-map-path (str js-path ".map")]
    (transform [ALL :path #(re-matches #"/js/\w+/app.js.map" %)]
               (constantly source-map-path)
               assets)))

(defn- all-optimizations [assets options]
  (-> assets
      (optimizations/minify-js-assets options)
      (optimizations/minify-css-assets options)
      (optimizations/concatenate-bundles)
      (optimizations/add-cache-busted-expires-headers)
      (optimizations/add-last-modified-headers)
      (fix-source-map-path)))

(defn get-assets []
  (concat
   (assets/load-bundle "public"
                       "libs.css"
                       ["/css/semantic.min.css"
                        "/css/calendar.min.css"])
   (assets/load-bundle "public"
                       "libs.js"
                       ["/js/jquery.min.js" ; 3.1.1
                        "/js/semantic.min.js" ; 2.4.1
                        "/js/calendar.min.js" ; 0.0.8 (https://github.com/mdehoog/Semantic-UI-Calendar)
                        "/js/stacktrace.min.js"]) ; 2.0.0
   (assets/load-bundle "public" "app.css" ["/css/app.css"])
   (assets/load-bundle "public" "app.js" ["/js/app.js"])
   (assets/load-assets "public" ["/js/app.js.map"])))

(defn wrap-asset-pipeline [handler]
  (optimus/wrap handler
                get-assets
                (if (env? :development) optimizations/none all-optimizations)
                (if (env? :development) strategies/serve-live-assets strategies/serve-frozen-assets)))
