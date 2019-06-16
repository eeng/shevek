(defproject shevek "0.5.1"
  :description "Interactive data exploration UI for Druid, aimed at end users."
  :url "https://github.com/eeng/shevek"
  :license {:name "MIT License"
            :url "https://opensource.org/licenses/MIT"}

  :dependencies [;; Backend
                 [org.clojure/clojure "1.10.0"]
                 [mount "0.1.12"]
                 [cprop "0.1.11"]
                 [http-kit "2.2.0"]
                 [cheshire "5.7.0"] ; Needed for the :as :json option of clj-http
                 [ring/ring-defaults "0.3.2"]
                 [ring-middleware-format "0.7.2" :exclusions [org.clojure/tools.reader]] ; La exclusion es xq clojurescript 1.10 necesita una versión mas nueva
                 [buddy/buddy-sign "1.5.0"]
                 [buddy/buddy-auth "1.4.1"]
                 [bcrypt-clj "0.3.3"]
                 [clj-time "0.14.0"]
                 [com.taoensso/timbre "4.10.0"]
                 [compojure "1.5.2"]
                 [overtone/at-at "1.2.0"]
                 [com.draines/postal "2.0.2"]
                 [com.novemberain/monger "3.5.0" :exclusions [com.google.guava/guava]] ; Sin la exclusion usaba guava 18 con el cual no es compatible clojurescript 1.10
                 [org.clojure/tools.nrepl "0.2.13"]
                 [clj-http "2.3.0"]
                 [org.clojure/core.match "0.3.0"]
                 [ns-tracker "0.3.1"] ; For the reloader
                 [hiccup "1.0.5"]
                 [optimus "0.20.1"]
                 [clj-fakes "0.11.0"]
                 [influxdb/influxdb-clojure "0.2.0" :exclusions [com.google.guava/guava]]

                 ;; Frontend
                 [org.clojure/clojurescript "1.10.238"]
                 [org.clojure/core.async "0.4.490"]
                 [reagent "0.8.1"]
                 [cljs-ajax "0.7.4"]
                 [secretary "1.2.3"]
                 [kibu/pushy "0.3.8"]
                 [com.andrewmcveigh/cljs-time "0.5.1"]
                 [tongue "0.2.6"]
                 [cljsjs/numeral "2.0.6-0"]
                 [cljsjs/clipboard "1.6.1-1"]
                 [cljsjs/chartjs "2.7.3-0"]
                 [org.slf4j/slf4j-nop "1.7.12"] ; To disable monger log messages
                 [cljsjs/filesaverjs "1.3.3-0"]
                 [testdouble/clojurescript.csv "0.3.0"]
                 [cljsjs/react-grid-layout "0.16.6-0"]

                 ;; Shared
                 [funcool/cuerdas "2.0.5"]
                 [com.rpl/specter "1.1.1"]
                 [prismatic/schema "1.1.10"]
                 [metosin/schema-tools "0.11.0"]]

  :plugins [[lein-pprint "1.1.2"]
            [lein-cooper "1.2.2"]
            [lein-figwheel "0.5.18"]
            [lein-cljsbuild "1.1.7"]
            [deraen/lein-less4j "0.6.2"]]

  :source-paths ["src/clj" "src/cljc"]
  :resource-paths ["resources"]
  :test-paths ["test/clj" "test/cljc"]
  :target-path "target/%s"
  :uberjar-name "shevek.jar"
  :main shevek.app

  :jvm-opts ["-Djava.awt.headless=true"] ; Otherwise optimus would show the dock java icon

  :cooper {"backend" ["lein" "run" "-m" "shevek.app/start"]
           "less" ["lein" "less4j" "auto"]
           "figwheel" ["lein" "figwheel"]}

  :less {:source-paths ["src/less"]
         :source-map true}

  :figwheel {:css-dirs ["out/dev/public/css"]
             :server-logfile "log/figwheel.log"
             :repl false ; So we can start figwheel through cooper
             :nrepl-port 4002
             :nrepl-middleware ["cider.piggieback/wrap-cljs-repl"]}

  :cljsbuild {:builds
              {:app
               {:source-paths ["src/cljs"]
                :compiler {:install-deps true
                           :npm-deps {:semantic-ui-calendar "0.0.8"
                                      :stacktrace-js "2.0.0"
                                      :semantic-ui-css "2.4.1"
                                      :jquery "3.4.1"
                                      :simplebar "3.1.3"
                                      :javascript-detect-element-resize "0.5.3"}}}}}

  :test-selectors {:default (complement :acceptance)
                   :acceptance :acceptance
                   :all (constantly true)}

  :test-refresh {:quiet true
                 :changes-only true
                 :notify-command ["terminal-notifier" "-title" "Tests" "-message"]}

  :aliases {"build-frontend" ["do" ["cljsbuild" "once"] ["less4j" "once"]]
            "package" ["with-profile" "prod" "do" "clean," "build-frontend," "uberjar"]
            "backend-tests" ["with-profile" "backend-tests" "test"]
            "backend-testing" ["with-profile" "backend-tests" "test-refresh"]
            "frontend-tests" ["with-profile" "frontend-tests" "doo" "chrome-headless" "once"]
            "frontend-testing" ["with-profile" "frontend-tests" "doo" "chrome-headless" "auto"]
            "acceptance-tests" ["with-profile" "acceptance-tests" "do" "build-frontend" ["test" ":acceptance"]]
            "acceptance-tests-repl" ["with-profile" "dev-acceptance-tests" "repl" ":start" ":port" "4101"]
            "ci" ["do" "backend-tests," "frontend-tests," "acceptance-tests"]}

  :profiles {:dev
             {:dependencies [[org.clojure/tools.namespace "0.2.11"]
                             [proto-repl "0.3.1"]
                             [binaryage/devtools "0.9.10"]
                             ; ClojureScript REPL on top of nREPL
                             [cider/piggieback "0.3.8"]
                             [figwheel-sidecar "0.5.16"]]
              :jvm-opts ["-Dconf=dev/resources/config.edn"]
              :source-paths ["dev/clj"]
              :resource-paths ["out/dev"]
              :cljsbuild {:builds
                          {:app
                           {:figwheel {:on-jsload "shevek.app/reload"}
                            :compiler {:main shevek.app
                                       :output-dir "out/dev/public/js/out"
                                       :output-to "out/dev/public/js/app.js"
                                       :asset-path "/js/out"
                                       :source-map-timestamp true
                                       :preloads [devtools.preload]}}
                           :test
                           {:source-paths ["src/cljs" "test/cljs"]
                            :compiler {:output-to "out/test/unit-test.js"
                                       :output-dir "out/test/public/js/out"
                                       :main shevek.runner
                                       :optimizations :whitespace
                                       :verbose false
                                       :pretty-print true}}}}
              :less {:target-path "out/dev/public/css"}
              :clean-targets ^{:protect false} ["target/dev" "out/dev"]}

             :minimized-frontend
             {:cljsbuild {:builds
                          {:app
                           {:compiler {:optimizations :advanced
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

             :shared-test
             {:resource-paths ["test/resources"]
              :dependencies [[etaoin "0.3.2"]
                             [se.haleby/stub-http "0.2.7"]
                             [prismatic/schema-generators "0.1.2"]]}

             :backend-tests
             [:shared-test
              {:plugins [[com.jakemccrary/lein-test-refresh "0.24.1"]
                         [venantius/ultra "0.5.2"]]}]

             :frontend-tests
             {:plugins [[lein-doo "0.1.10"]]
              :dependencies [[pjstadig/humane-test-output "0.8.3"]
                             [cljsjs/jquery "3.2.1-0"]]
              :cljsbuild {:builds
                          {:app
                           {:source-paths ["src/cljs" "test/cljs"]
                            :compiler {:output-to "out/utest/unit-test.js"
                                       :output-dir "out/utest/public/js/out"
                                       :main shevek.runner
                                       :optimizations :whitespace
                                       :verbose false
                                       :pretty-print true}}}}
              :doo {:paths {:karma "./node_modules/karma/bin/karma"}
                    :build "app"}}

             :acceptance-tests
             [:shared-test
              :minimized-frontend
              {:cljsbuild {:builds
                           {:app
                            {:compiler {:output-dir "out/atest/public/js/out"
                                        :output-to "out/atest/public/js/app.js"
                                        :source-map "out/atest/public/js/app.js.map"}}}}
               :less {:target-path "out/atest/public/css"}
               :resource-paths ["test/resources" "out/atest"]}]

             ; Allows to run individual acceptance tests from the repl, using the dev frontend
             :dev-acceptance-tests
             [:shared-test
              :dev
              :base] ; Otherwise an error is raised when opening the repl

             :prod
             [:minimized-frontend
              {:resource-paths ["out/prod"]
               :cljsbuild {:builds
                           {:app
                            {:compiler {:output-dir "out/prod/public/js/out"
                                        :output-to "out/prod/public/js/app.js"
                                        :source-map "out/prod/public/js/app.js.map"}}}}
               :less {:target-path "out/prod/public/css"}
               :clean-targets ^{:protect false} ["out/prod" "target/prod" "target/prod+f27bb53b" "target/uberjar" "target/uberjar+prod"]}]

             :uberjar {:aot :all :auto-clean false}})
