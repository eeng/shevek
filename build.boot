(set-env!
  :source-paths   #{"src/clj" "src/cljs" "src/less"}
  :resource-paths #{"resources"}
  :dependencies   '[[org.clojure/clojurescript "1.9.494"]
                    [adzerk/boot-cljs "1.7.228-2"]
                    [pandeiro/boot-http "0.7.0"]
                    [adzerk/boot-reload "0.5.1"]
                    [com.cemerick/piggieback "0.2.1" :scope "test"] ; Needed by boot-cljs-repl
                    [weasel "0.7.0" :scope "test"] ; Needed by boot-cljs-repl
                    [org.clojure/tools.nrepl "0.2.12" :scope "test"] ; Needed by boot-cljs-repl
                    [adzerk/boot-cljs-repl "0.3.3"]
                    [deraen/boot-less "0.6.2" :scope "test"]
                    [samestep/boot-refresh "0.1.0" :scope "test"]
                    [proto-repl "0.3.1"]
                    [reagent "0.6.0"]
                    [clj-http "2.3.0"]
                    [cheshire "5.7.0"] ; Needed for the :as :json option of clj-http
                    [tongue "0.1.4"]
                    [mount "0.1.11"]
                    [http-kit "2.2.0"]
                    [cprop "0.1.10"]])

(require
 '[adzerk.boot-cljs :refer [cljs]]
 '[adzerk.boot-cljs-repl :refer [cljs-repl start-repl]]
 '[adzerk.boot-reload :refer [reload]]
 '[pandeiro.boot-http :refer [serve]]
 '[deraen.boot-less :refer [less]]
 '[samestep.boot-refresh :refer [refresh]])

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
        (sift :move {#"app.css" "css/app.css" #"app.main.css.map" "css/app.main.css.map"})
        (target)))

(deftask dev-config []
  (set-env! :source-paths #(conj % "dev/clj"))
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
        (repl :server true)
        (reload)
        (cljs-repl)
        (build)))

(deftask dev-run
  "Runs the app in development mode, without REPL and code reloading."
  []
  (comp (dev-config)
        (build)
        (run :main-namespace "pivot.app")
        (wait)))
