(set-env!
  :source-paths   #{"src/clj" "src/cljc" "src/cljs" "src/less"}
  :resource-paths #{"resources"}
  :dependencies   '[[org.clojure/clojurescript "1.9.494"]
                    [adzerk/boot-cljs "1.7.228-2" :scope "test"]
                    [adzerk/boot-reload "0.5.1" :scope "test"]
                    [com.cemerick/piggieback "0.2.1" :scope "test"] ; Needed by boot-cljs-repl
                    [weasel "0.7.0" :scope "test"] ; Needed by boot-cljs-repl
                    [org.clojure/tools.nrepl "0.2.12" :scope "test"] ; Needed by boot-cljs-repl
                    [adzerk/boot-cljs-repl "0.3.3" :scope "test"]
                    [deraen/boot-less "0.6.2" :scope "test"]
                    [samestep/boot-refresh "0.1.0" :scope "test"]
                    [metosin/boot-alt-test "0.3.0" :scope "test"]
                    [crisptrutski/boot-cljs-test "0.3.0" :scope "test"]
                    [binaryage/devtools "0.9.2" :scope "test"]
                    [powerlaces/boot-cljs-devtools "0.2.0" :scope "test"]
                    [doo "0.1.7" :scope "test"] ; Needed by boot-cljs-test
                    [proto-repl "0.3.1"]
                    [reagent "0.6.0"]
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
                    [com.taoensso/timbre "4.8.0"]])

(require
 '[adzerk.boot-cljs :refer [cljs]]
 '[adzerk.boot-cljs-repl :refer [cljs-repl start-repl]]
 '[adzerk.boot-reload :refer [reload]]
 '[deraen.boot-less :refer [less]]
 '[samestep.boot-refresh :refer [refresh]]
 '[metosin.boot-alt-test :refer [alt-test]]
 '[crisptrutski.boot-cljs-test :refer [test-cljs]]
 '[powerlaces.boot-cljs-devtools :refer [cljs-devtools]])

(deftask run
  "Run the -main function in some namespace with arguments."
  [m main-namespace NAMESPACE str   "The namespace containing a -main function to invoke."
   a arguments      EXPR      [edn] "An optional argument sequence to apply to the -main function."]
  (with-pre-wrap fs
    (require (symbol main-namespace) :reload)
    (if-let [f (resolve (symbol main-namespace "-main"))]
      (apply f arguments)
      (throw (ex-info "No -main method found" {:main-namespace main-namespace})))
    fs))

(deftask build []
  (comp (cljs)
        (less)
        (sift :move {#"app.css" "public/css/app.css" #"app.main.css.map" "public/css/app.main.css.map"})
        (target)))

(deftask dev-config []
  (merge-env! :source-paths #{"dev/clj"})
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
        (build)))

(deftask dev-run
  "Runs the application in development mode, without REPL and code reloading."
  []
  (comp (dev-config)
        (build)
        (run :main-namespace "pivot.app")
        (wait)))

(deftask clj-testing
  "Continuos automatic testing of the backend."
  []
  (merge-env! :source-paths #{"test/clj"}
              :resource-paths #{"test/resources"})
  (comp (dev)
        (alt-test)))

(deftask cljs-testing
  "Continuos automatic testing of the frontend."
  []
  (merge-env! :source-paths #{"test/cljs"}
              :resource-paths #{"test/resources"})
  (comp (dev)
        (test-cljs)))

(deftask testing
  "Continuos automatic testing."
  []
  (merge-env! :source-paths #{"test/clj" "test/cljs"}
              :resource-paths #{"test/resources"})
  (comp (dev)
        (alt-test)
        (test-cljs)))
