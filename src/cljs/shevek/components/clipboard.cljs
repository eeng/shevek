(ns shevek.components.clipboard
  (:require [reagent.core :as r]
            [cljsjs.clipboard]))

(defn clipboard-button [button]
  (let [clipboard-atom (atom nil)]
    (r/create-class
     {:component-did-mount #(let [clipboard (new js/Clipboard (r/dom-node %))]
                              (reset! clipboard-atom clipboard))
      :component-will-unmount #(when @clipboard-atom
                                 (.destroy @clipboard-atom)
                                 (reset! clipboard-atom nil))
      :reagent-render (fn [] button)})))
