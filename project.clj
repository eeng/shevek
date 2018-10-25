(defproject shevek "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [;; Backend
                 [org.clojure/clojure "1.8.0"]
                 [mount "0.1.12"]
                 [cprop "0.1.11"]
                 [http-kit "2.2.0"]
                 [cheshire "5.7.0"] ; Needed for the :as :json option of clj-http
                 [ring/ring-defaults "0.2.3"]
                 [ring-middleware-format "0.7.2" :exclusions [org.clojure/tools.reader]] ; La exclusion es xq clojurescript 1.10 necesita una versión mas nueva
                 [buddy/buddy-sign "1.5.0"]
                 [buddy/buddy-auth "1.4.1"]
                 [bcrypt-clj "0.3.3"]
                 [clj-time "0.14.0"]
                 [com.taoensso/timbre "4.10.0"]
                 [compojure "1.5.2"]
                 [overtone/at-at "1.2.0"]
                 [com.draines/postal "2.0.2"]
                 [com.novemberain/monger "3.1.0" :exclusions [com.google.guava/guava]] ; Sin la exclusion usaba guava 18 con el cual no es compatible clojurescript 1.10
                 [org.clojure/tools.nrepl "0.2.13"]
                 [clj-http "2.3.0"]
                 [org.clojure/core.match "0.3.0-alpha4"]
                 [ns-tracker "0.3.1"] ; For the reloader
                 [hiccup "1.0.5"]
                 [optimus "0.20.1"]
                 [clj-fakes "0.11.0"]

                 ;; Frontend
                 [org.clojure/clojurescript "1.10.238"]
                 [org.clojure/core.async "0.4.474"]
                 [reagent "0.7.0" :exclusions [cljsjs/react]] ; Have to use 0.7 with the exclusion and react-with-addons otherwise the frontend unit test don't work because the latest reagent use a newer react with ES6 features, that aren't supported on phantom as of today
                 [cljsjs/react-with-addons "15.6.1-0"]
                 [cljs-ajax "0.7.4"]
                 [secretary "1.2.3"]
                 [kibu/pushy "0.3.8"]
                 [com.andrewmcveigh/cljs-time "0.5.1"]
                 [tongue "0.1.4"]
                 [cljsjs/numeral "2.0.6-0"]
                 [cljsjs/jwt-decode "2.1.0-0"]
                 [cljsjs/clipboard "1.6.1-1"]
                 [cljsjs/chartjs "2.6.0-0"]
                 [org.slf4j/slf4j-nop "1.7.12"] ; To disable monger log messages
                 [cljsjs/filesaverjs "1.3.3-0"]
                 [testdouble/clojurescript.csv "0.3.0"]

                 ;; Shared
                 [funcool/cuerdas "2.0.5"]
                 [com.rpl/specter "1.1.1"]
                 [prismatic/schema "1.1.7"]
                 [metosin/schema-tools "0.9.1"]]

  :plugins [[lein-pprint "1.1.2"]
            [lein-cooper "1.2.2"]
            [lein-figwheel "0.5.16"]
            [lein-cljsbuild "1.1.7"]
            [deraen/lein-less4j "0.6.2"]]

  :source-paths ["src/clj" "src/cljc"]
  :resource-paths ["resources"]
  :test-paths ["test/clj" "test/cljc"]
  :target-path "target/%s"
  :uberjar-name "shevek.jar"
  :main shevek.app

  :clean-targets ^{:protect false} ["resources/public/js/out" "resources/public/js/app.js"
                                    "resources/public/css/app.css" "resources/public/css/app.main.css.map"
                                    "resources/private"
                                    :target-path]

  :jvm-opts ["-Djava.awt.headless=true"] ; Otherwise optimus would show the dock java icon

  :cooper {"backend" ["lein" "run" "-m" "shevek.app/start-for-dev"]
           "less" ["lein" "less4j" "auto"]
           "figwheel" ["lein" "figwheel"]}

  :less {:source-paths ["src/less"]
         :target-path "resources/public/css"
         :source-map true}

  :figwheel {:css-dirs ["resources/public/css"]
             :server-logfile "log/figwheel.log"
             :repl false ; So we can start figwheel through cooper
             :nrepl-port 4002
             :nrepl-middleware ["cider.piggieback/wrap-cljs-repl"]}

  :cljsbuild {:builds
              {:app
               {:source-paths ["src/cljs"]
                :compiler {:output-dir "resources/public/js/out"
                           :output-to "resources/public/js/app.js"
                           :install-deps true
                           :npm-deps {:semantic-ui-calendar "0.0.8"
                                      :stacktrace-js "2.0.0"
                                      :semantic-ui-css "2.4.1"
                                      :jquery "3.3.1"}}}}}

  :test-selectors {:default (complement :acceptance)
                   :acceptance :acceptance
                   :all (constantly true)}

  :test-refresh {:quiet true
                 :changes-only true
                 :notify-command ["terminal-notifier" "-title" "Tests" "-message"]}

  :aliases {"frontend-testing" ["doo" "phantom" "test" "auto"]
            "backend-testing" ["with-profile" "+ultra" "test-refresh"]
            "build-frontend" ["with-profile" "prod" "do" ["cljsbuild" "once"] ["less4j" "once"]]
            "package" ["do" ["clean"] "build-frontend" "uberjar"]
            "ci" ["do" ["clean"] "test" ["doo" "phantom" "test" "once"] "build-frontend" ["test" ":acceptance"]]}

  :profiles {:dev {:source-paths ["dev/clj"]
                   :jvm-opts ["-Dconf=dev/resources/config.edn"]
                   :resource-paths ["test/resources"]
                   :dependencies [[org.clojure/tools.namespace "0.2.11"]
                                  [proto-repl "0.3.1"]
                                  [binaryage/devtools "0.9.10"]
                                  [cider/piggieback "0.3.8"] ; ClojureScript REPL on top of nREPL
                                  [figwheel-sidecar "0.5.16"] ; ClojureScript REPL on top of nREPL
                                  ;; Testing clj
                                  [etaoin "0.2.1"]
                                  [se.haleby/stub-http "0.2.1"]
                                  [prismatic/schema-generators "0.1.0"]
                                  ;; Testing cljs
                                  [pjstadig/humane-test-output "0.8.3"]
                                  [cljsjs/jquery "3.2.1-0"]]
                   :plugins [[com.jakemccrary/lein-test-refresh "0.23.0"]
                             [lein-doo "0.1.10"]
                             [cider/cider-nrepl "0.18.0-SNAPSHOT"]] ; For Calva
                   :cljsbuild {:builds
                               {:app
                                {:figwheel true
                                 :compiler {:main shevek.app
                                            :asset-path "/js/out"
                                            :source-map-timestamp true
                                            :preloads [devtools.preload]}}
                                :test
                                {:source-paths ["src/cljs" "test/cljs"]
                                 :compiler {:output-to "resources/private/js/unit-test.js"
                                            :main shevek.runner
                                            :optimizations :whitespace
                                            :verbose false
                                            :pretty-print true}}}}}
             :prod {:cljsbuild {:builds
                                {:app
                                 {:compiler {:source-map "resources/public/js/app.js.map"
                                             :optimizations :advanced
                                             :pretty-print false
                                             :source-map-timestamp false
                                             :infer-externs true
                                             :closure-warnings {:externs-validation :off :non-standard-jsdoc :off}
                                             :closure-defines {"goog.DEBUG" false}
                                             :externs ["src/externs/jquery.js"
                                                       "src/externs/semantic-ui.js"
                                                       "src/externs/calendar.js"]}}}}
                    :less {:source-map false
                           :compression true}}
             :uberjar {:aot :all
                       :auto-clean false}
             ; Put ultra into a separate profile to active it only during clj testing, otherwise cljs testing throws an error due to this bug: https://github.com/emezeske/lein-cljsbuild/issues/469
             :ultra {:plugins [[venantius/ultra "0.5.2"]]}})
