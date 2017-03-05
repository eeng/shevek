(set-env!
  :source-paths   #{"src/cljs"}
  :resource-paths #{"resources"}
  :dependencies   '[[org.clojure/clojurescript "1.9.494"]
                    [adzerk/boot-cljs "1.7.228-2"]
                    [pandeiro/boot-http "0.7.0"]
                    [adzerk/boot-reload "0.5.1"]
                    [com.cemerick/piggieback "0.2.1" :scope "test"] ; needed by boot-cljs-repl
                    [weasel "0.7.0" :scope "test"] ; needed by boot-cljs-repl
                    [org.clojure/tools.nrepl "0.2.12" :scope "test"] ; needed by boot-cljs-repl
                    [deraen/boot-less "0.6.2" :scope "test"]
                    [adzerk/boot-cljs-repl "0.3.3"]
                    [proto-repl "0.3.1"]])

(require
 '[adzerk.boot-cljs      :refer [cljs]]
 '[adzerk.boot-cljs-repl :refer [cljs-repl start-repl]]
 '[adzerk.boot-reload    :refer [reload]]
 '[pandeiro.boot-http    :refer [serve]]
 '[deraen.boot-less      :refer [less]])

(deftask build []
  (comp (notify :visual true :title "App")
        (cljs)
        (target :dir #{"target"})))
        ;(less)))
        ;(sift :move {#"less.css" "css/less.css" #"less.main.css.map" "css/less.main.css.map"})))

(deftask frontend []
  (task-options! cljs   {:optimizations :none :source-map true}
                 reload {:on-jsload 'pivot.app/init})
  ;less   {:source-map  true})
  (comp (serve :dir "target")
        (watch)
        (reload)
        (cljs-repl :nrepl-opts {:port 9009})
        (build)))

(deftask dev
  "Profile setup for development with Proto REPL"
  []
  (println "Dev profile running")
  (set-env!
   :init-ns 'user
   :source-paths #(into % ["dev"]))

  ;; Makes clojure.tools.namespace.repl work per https://github.com/boot-clj/boot/wiki/Repl-reloading
  (require 'clojure.tools.namespace.repl)
  (eval '(apply clojure.tools.namespace.repl/set-refresh-dirs (get-env :directories)))

  identity)
