(set-env!
  :source-paths   #{"src/clj" "src/cljc" "src/cljs" "src/less"}
  :resource-paths #{"resources"}
  :dependencies   '[[org.clojure/clojurescript "1.9.671"]
                    [adzerk/boot-cljs "2.0.0" :scope "test"]
                    [adzerk/boot-reload "0.5.1" :scope "test"]
                    [com.cemerick/piggieback "0.2.1" :scope "test"] ; Needed by boot-cljs-repl
                    [weasel "0.7.0" :scope "test"] ; Needed by boot-cljs-repl
                    [org.clojure/tools.nrepl "0.2.12" :scope "test"] ; Needed by boot-cljs-repl
                    [adzerk/boot-cljs-repl "0.3.3"]
                    [deraen/boot-less "0.6.2" :scope "test"]
                    [samestep/boot-refresh "0.1.0" :scope "test"]
                    [metosin/boot-alt-test "0.3.2" :scope "test"]
                    [crisptrutski/boot-cljs-test "0.3.0" :scope "test"]
                    [binaryage/devtools "0.9.2" :scope "test"]
                    [powerlaces/boot-cljs-devtools "0.2.0" :scope "test"]
                    [doo "0.1.7" :scope "test"] ; Needed by boot-cljs-test
                    [pjstadig/humane-test-output "0.8.1" :scope "test"]
                    [etaoin "0.1.6" :scope "test"]
                    [proto-repl "0.3.1"]
                    [reagent "0.7.0"]
                    [clj-http "2.3.0"]
                    [cheshire "5.7.0"] ; Needed for the :as :json option of clj-http
                    [tongue "0.1.4"]
                    [mount "0.1.11"]
                    [http-kit "2.2.0"]
                    [cprop "0.1.10"]
                    [ring/ring-defaults "0.2.3"]
                    [ring-middleware-format "0.7.2"]
                    [compojure "1.5.2"]
                    [se.haleby/stub-http "0.2.1"]
                    [cljs-ajax "0.5.8"]
                    [secretary "1.2.3"]
                    [funcool/cuerdas "2.0.3"]
                    [com.andrewmcveigh/cljs-time "0.5.0-alpha2"]
                    [com.taoensso/timbre "4.8.0"]
                    [com.rpl/specter "1.0.0"]
                    [prismatic/schema "1.1.6"]
                    [metosin/schema-tools "0.9.0"]
                    [prismatic/schema-generators "0.1.0"]
                    [com.novemberain/monger "3.1.0"]
                    [cljsjs/numeral "2.0.6-0"]
                    [org.clojure/core.match "0.3.0-alpha4"]
                    [spyscope "0.1.5"]
                    [bcrypt-clj "0.3.3"]
                    [buddy/buddy-sign "1.5.0"]
                    [buddy/buddy-auth "1.4.1"]
                    [cljsjs/jwt-decode "2.1.0-0"]])

(require
 '[adzerk.boot-cljs :refer [cljs]]
 '[adzerk.boot-cljs-repl :refer [cljs-repl start-repl]]
 '[adzerk.boot-reload :refer [reload]]
 '[deraen.boot-less :refer [less]]
 '[samestep.boot-refresh :refer [refresh]]
 '[metosin.boot-alt-test :refer [alt-test]]
 '[crisptrutski.boot-cljs-test :refer [test-cljs] :rename {test-cljs alt-test-cljs}]
 '[powerlaces.boot-cljs-devtools :refer [cljs-devtools]]
 '[shevek.app])

(deftask run
  "Run the -main function in some namespace with arguments."
  [m main-namespace NAMESPACE str   "The namespace containing a -main function to invoke."
   a arguments      EXPR      [edn] "An optional argument sequence to apply to the -main function."]
  (with-pre-wrap fs
    (require (symbol main-namespace))
    (if-let [f (resolve (symbol main-namespace "-main"))]
      (apply f arguments)
      (throw (ex-info "No -main method found" {:main-namespace main-namespace})))
    fs))

(deftask build
  "Build ClojureScript a Less files."
  []
  (comp (cljs)
        (less)
        (sift :move {#"app.css" "public/css/app.css" #"app.main.css.map" "public/css/app.main.css.map"})
        (target)))

(deftask build-and-start-app-for-dev []
  (task-options! target {:dir #{"target/dev"}})
  (comp (build)
        (with-pass-thru _
          (shevek.app/dev-start))))

(deftask dev-config []
  (merge-env! :source-paths #{"dev/clj"} :resource-paths #{"dev/resources"})
  (System/setProperty "conf" "dev/resources/config.edn")
  (task-options! cljs {:optimizations :none :source-map true}
                 less {:source-map  true})
  identity)

(deftask dev
  "Runs the application in development mode with REPL and automatic code reloading."
  []
  (comp (dev-config)
        (watch)
        (notify :visual true :title "App")
        (refresh)
        (reload :asset-path "public")
        (cljs-repl)
        (cljs-devtools)
        (build-and-start-app-for-dev)))

(deftask dev-run
  "Runs the application in development mode, without REPL and code reloading."
  []
  (comp (dev-config)
        (build-and-start-app-for-dev)
        (wait)))

(deftask test-config []
  (merge-env! :source-paths #{"test/clj" "test/cljc" "test/cljs"} :resource-paths #{"test/resources"})
  (System/setProperty "conf" "test/resources/config.edn")
  (task-options! cljs {:optimizations :none :source-map true}
                 less {:source-map  true})
  identity)

(deftask test-clj
  "Run the backend tests."
  []
  (comp (test-config)
        (alt-test :test-matcher #"^(?!.*acceptance).*"
                  :on-start 'shevek.test-helper/init-unit-tests)))

(deftask test-cljs
  "Run the frontend tests."
  []
  (comp (test-config)
        (alt-test-cljs)))

(deftask test-acceptance
  "Run the acceptance tests."
  []
  (task-options! target {:dir #{"target/test"}})
  (comp (test-config)
        (build)
        ; Hay que levantar la app (con nrepl) desde el on-start y no desde boot porque sino el test al correr en un pod no ve los mount states.
        (alt-test :test-matcher #".*acceptance\.(?!test\-helper).*"
                  :on-start 'shevek.test-helper/init-acceptance-tests)))

(deftask test-all
  "Run all tests."
  []
  (comp (test-clj)
        (alt-test-cljs :exit? true)
        (test-acceptance)))
