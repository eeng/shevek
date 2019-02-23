(ns shevek.components.shortcuts
  (:require [reagent.core :as r]))

(def keys-translation
  {13 :enter
   27 :escape})

(defn- handle-keypressed [shortcuts-map e]
  (let [key (-> e .-which keys-translation)
        assigned-fn (shortcuts-map key)
        textarea? (-> e .-target .-tagName (= "TEXTAREA"))
        dropdown? (-> e .-target js/$ (.closest ".dropdown.visible") .-length (> 0))]
    (when (and assigned-fn
               (not textarea?) ; Keep the enter default function on a textarea which adds a new line
               (not dropdown?)) ; Only handle the shortcuts when the dropdown is not unfolded
      (assigned-fn))))

(defn shortcuts [shortcuts-map child]
  (r/create-class
   {:reagent-render
    (fn [_ child] child)
    :component-did-mount
    (fn [this]
      (-> this r/dom-node js/$ (.on "keyup" (partial handle-keypressed shortcuts-map))))}))
