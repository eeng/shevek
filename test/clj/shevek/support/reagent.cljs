(ns shevek.support.reagent
  (:require [cljsjs.jquery]
            [reagent.core :as r]
            [cljs-react-test.utils :as tu]))

(def ^:dynamic container)

(defn with-container [test-fn]
  (binding [container (tu/new-container!)]
    (test-fn)
    (tu/unmount! container)))

(defn render-component [comp]
  (r/render-component comp container))

(defn texts
  ([selector]
   (->> selector js/$ .toArray (map #(.-textContent %))))
  ([parent-selector child-selector]
   (->> parent-selector js/$ .toArray
        (map (fn [parent]
               (->> (js/$ child-selector parent) .toArray
                    (map #(.-textContent %))))))))
