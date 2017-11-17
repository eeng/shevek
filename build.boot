(set-env!
  :source-paths   #{"src/clj" "src/cljc" "src/cljs" "src/less"}
  :resource-paths #{"resources"}
  :dependencies   '[[org.clojure/clojurescript "1.9.671"]
                    [adzerk/boot-cljs "2.0.0" :scope "test"]
                    [adzerk/boot-reload "0.5.1" :scope "test"]
                    [com.cemerick/piggieback "0.2.1" :scope "test"] ; Needed by boot-cljs-repl
                    [weasel "0.7.0" :scope "test"] ; Needed by boot-cljs-repl
                    [org.clojure/tools.nrepl "0.2.12"]
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
                    [reagent "0.7.0" :exclusions [cljsjs/react]]
                    [clj-http "2.3.0"]
                    [cheshire "5.7.0"] ; Needed for the :as :json option of clj-http
                    [tongue "0.1.4"]
                    [mount "0.1.11"]
                    [http-kit "2.2.0"]
                    [cprop "0.1.11"]
                    [ring/ring-defaults "0.2.3"]
                    [ring-middleware-format "0.7.2"]
                    [compojure "1.5.2"]
                    [se.haleby/stub-http "0.2.1"]
                    [cljs-ajax "0.5.8"]
                    [secretary "1.2.3"]
                    [kibu/pushy "0.3.8"]
                    [funcool/cuerdas "2.0.3"]
                    [clj-time "0.14.0"]
                    [com.andrewmcveigh/cljs-time "0.5.1"]
                    [com.taoensso/timbre "4.8.0"]
                    [com.rpl/specter "1.0.4"]
                    [prismatic/schema "1.1.7"]
                    [metosin/schema-tools "0.9.1"]
                    [prismatic/schema-generators "0.1.0"]
                    [com.novemberain/monger "3.1.0"]
                    [cljsjs/numeral "2.0.6-0"]
                    [org.clojure/core.match "0.3.0-alpha4"]
                    [spyscope "0.1.5"]
                    [bcrypt-clj "0.3.3"]
                    [buddy/buddy-sign "1.5.0"]
                    [buddy/buddy-auth "1.4.1"]
                    [cljsjs/jwt-decode "2.1.0-0"]
                    [cljsjs/clipboard "1.6.1-1"]
                    [cljsjs/chartjs "2.6.0-0"]
                    [lukesnape/boot-asset-fingerprint "1.5.1" :scope "test"]
                    [cljsjs/react-with-addons "15.6.1-0"]
                    [cljs-react-test "0.1.4-SNAPSHOT" :scope "test"]
                    [cljsjs/jquery "3.2.1-0" :scope "test"] ; Only needed for reagent tests as in the app we use the extern, otherwise there were dep issues
                    [overtone/at-at "1.2.0"]
                    [com.draines/postal "2.0.2"]])

(require
 '[adzerk.boot-cljs :refer [cljs]]
 '[adzerk.boot-cljs-repl :refer [cljs-repl start-repl]]
 '[adzerk.boot-reload :refer [reload]]
 '[deraen.boot-less :refer [less]]
 '[afrey.boot-asset-fingerprint :refer [asset-fingerprint]]
 '[samestep.boot-refresh :refer [refresh]]
 '[metosin.boot-alt-test :refer [alt-test]]
 '[crisptrutski.boot-cljs-test :refer [test-cljs] :rename {test-cljs alt-test-cljs}]
 '[powerlaces.boot-cljs-devtools :refer [cljs-devtools]]
 '[shevek.app])

(deftask run
  "Involke a function in some namespace with arguments."
  [n namespace NAMESPACE str   "The namespace containing the function to invoke."
   f function  FUNCTION  str   "The function to invoke. Leave empty to call -main."
   a arguments EXPR      [edn] "An optional argument sequence to apply to the function."]
  (with-pre-wrap fileset
    (require (symbol namespace))
    (let [function (or function "-main")
          f (resolve (symbol namespace function))]
      (if f
        (apply f arguments)
        (throw (ex-info "Function not found" {:namespace namespace :function function}))))
    fileset))

(deftask build-dev-frontend
  "Build ClojureScript and Less files."
  []
  (comp (cljs :optimizations :none :source-map true
              :compiler-options {:external-config {:devtools/config {:features-to-install [:formatters :hints :async]}}})
        (less :source-map true)
        (sift :move {#"app.css" "public/css/app.css" #"app.main.css.map" "public/css/app.main.css.map"})
        (asset-fingerprint :skip true)
        (target)))

(deftask build-and-start-app-for-dev []
  (comp (build-dev-frontend)
        (with-pass-thru _
          (shevek.app/start-without-nrepl))))

(deftask dev-config []
  (merge-env! :source-paths #{"dev/clj"} :resource-paths #{"dev/resources"})
  (System/setProperty "conf" "dev/resources/config.edn")
  (task-options! target {:dir #{"target/dev"}})
  identity)

(deftask dev
  "Runs the application in development mode with REPL and automatic code reloading."
  []
  (comp (dev-config)
        (watch)
        (notify :visual true :title "App")
        (refresh)
        (reload)
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
  (task-options! target {:dir #{"target/test"}})
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
  (comp (test-config)
        (build-dev-frontend)
        ; Hay que levantar la app (con nrepl) desde el on-start y no desde boot porque sino el test al correr en un pod no ve los mount states.
        (alt-test :test-matcher #".*acceptance\.(?!test\-helper).*"
                  :on-start 'shevek.test-helper/init-acceptance-tests)))

(deftask test-all
  "Run all tests."
  []
  (comp (test-clj)
        (alt-test-cljs :exit? true)))
        ; TODO reactivate acceptance
        ; (test-acceptance)))

(deftask seed
  "Seeds the application data."
  []
  (run :namespace "shevek.app" :function "seed"))

(deftask package
  "Package the project for deploy. Then start with: java -Dconf=dist/config.edn -jar dist/shevek.jar"
  []
  (comp (cljs :optimizations :advanced
              :compiler-options {:externs ["src/externs/jquery.js" "src/externs/semantic-ui.js" "src/externs/calendar.js"]
                                 :closure-defines {"goog.DEBUG" false}})
        (less)
        (sift :move {#"app.css" "public/css/app.css" #"app.main.css.map" "public/css/app.main.css.map"})
        (asset-fingerprint)
        (aot :namespace #{'shevek.app})
        (uber)
        (jar :file "shevek.jar" :main 'shevek.app)
        (sift :include #{#"shevek.jar"})
        (target :dir #{"dist"})))
