(ns shevek.support.reagent
  (:require [reagent.core :as r]
            [cljsjs.jquery]))

(def ^:dynamic container)

(defn- container-div []
  (let [id (str "container-" (gensym))
        node (.createElement js/document "div")]
    (set! (.-id node) id)
    [node id]))

(defn insert-container! [container]
  (.appendChild (.-body js/document) container))

(defn new-container! []
  (let [[n s] (container-div)]
    (insert-container! n)
    (.getElementById js/document s)))

(defn unmount!
  "Unmounts the React Component at a node"
  [n]
  (.unmountComponentAtNode js/ReactDOM n))

(defn with-container [test-fn]
  (binding [container (new-container!)]
    (test-fn)
    (unmount! container)))

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

(defn text [selector]
  (apply str (texts selector)))
