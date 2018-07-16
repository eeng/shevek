(defproject shevek "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [; Backend
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
                 [org.clojure/tools.nrepl "0.2.12"]
                 [clj-http "2.3.0"]
                 [org.clojure/core.match "0.3.0-alpha4"]
                 [ns-tracker "0.3.1"] ; For the reloader

                 ; Frontend
                 [org.clojure/clojurescript "1.10.238"]
                 [org.clojure/core.async "0.4.474"]
                 [reagent "0.8.1"]
                 [cljs-ajax "0.7.4"]
                 [secretary "1.2.3"]
                 [kibu/pushy "0.3.8"]
                 [com.andrewmcveigh/cljs-time "0.5.1"]
                 [tongue "0.1.4"]
                 [cljsjs/numeral "2.0.6-0"]
                 [cljsjs/jwt-decode "2.1.0-0"]
                 [cljsjs/clipboard "1.6.1-1"]
                 [cljsjs/chartjs "2.6.0-0"]

                 ; Shared
                 [funcool/cuerdas "2.0.3"]
                 [com.rpl/specter "1.0.4"]
                 [prismatic/schema "1.1.7"]
                 [metosin/schema-tools "0.9.1"]]

  :plugins [[lein-pprint "1.1.2"]
            [lein-cooper "1.2.2"]
            [lein-figwheel "0.5.16"]
            [lein-cljsbuild "1.1.7"]
            [deraen/lein-less4j "0.6.2"]]

  :source-paths ["src/clj" "src/cljc"]
  :resource-paths ["resources"] ; TODO LEIN mover luego a src asi queda uniforme
  :test-paths []
  :target-path "target/%s"
  :uberjar-name "shevek.jar"
  :main shevek.app

  :clean-targets ^{:protect false} [[:cljsbuild :builds :app :compiler :output-dir]
                                    [:cljsbuild :builds :app :compiler :output-to]
                                    :target-path]

  :aliases {"package" ["do"
                       ["with-profile" "less-uberjar" "less4j" "once"] ; Not very pretty but couldn't find other way to run the less task with compression
                       ["uberjar"]]}

  :cooper {"backend" ["lein" "run"]
           "less" ["lein" "less4j" "auto"]
           "figwheel" ["lein" "figwheel"]}

  :less {:source-paths ["src/less"]
         :target-path "resources/public/css"
         :source-map true}

  :cljsbuild {:builds
              {:app
               {:source-paths ["src/cljs"]
                :compiler {:output-to "resources/public/js/app.js"}}}}

  :figwheel {:css-dirs ["resources/public/css"]
             :server-logfile "log/figwheel.log"
             :repl false ; So we can start figwheel through cooper
             :nrepl-port 4002
             :nrepl-middleware ["cider.piggieback/wrap-cljs-repl"]}

  :profiles {:dev {:source-paths ["dev/clj"]
                   :jvm-opts ["-Dconf=dev/resources/config.edn"]
                   :dependencies [[org.clojure/tools.namespace "0.2.11"]
                                  [proto-repl "0.3.1"]
                                  [binaryage/devtools "0.9.10"]
                                  [cider/piggieback "0.3.8"]] ; ClojureScript REPL on top of nREPL
                   :cljsbuild {:builds
                               {:app
                                {:figwheel true
                                 :compiler {:main shevek.app
                                            :output-dir "resources/public/js/out"
                                            :asset-path "js/out"
                                            :source-map-timestamp true
                                            :preloads [devtools.preload]}}}}}
             :uberjar {:aot :all
                       :cljsbuild {:builds
                                   {:app
                                    {:compiler {:optimizations :advanced
                                                :pretty-print false
                                                :source-map-timestamp false
                                                :closure-defines {"goog.DEBUG" false}
                                                :externs ["src/externs/jquery.js" "src/externs/semantic-ui.js" "src/externs/calendar.js"]}}}}
                       :hooks [leiningen.cljsbuild]}
             :less-uberjar {:less {:source-map false
                                   :compression true}}})
