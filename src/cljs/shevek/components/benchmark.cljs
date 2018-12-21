(ns shevek.components.benchmark
  (:require [reagent.core :as r]))

(defn benchmark [component]
  (r/create-class {:reagent-render component
                   :component-will-mount #(js/console.time "Mounting")
                   :component-did-mount #(js/console.timeEnd "Mounting")
                   :component-will-update #(js/console.time "Updating")
                   :component-did-update #(js/console.timeEnd "Updating")}))
