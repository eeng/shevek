(ns shevek.components.modal2
  (:require [reagent.core :as r]))

(defn modal [{:keys [visible] :as props} & children]
  (let [opts (merge {:detachable false
                     :observeChanges true}
                    (dissoc props :visible :class))]
    (r/create-class
     {:display-name "modal"
      :component-did-mount #(-> % r/dom-node js/$ (.modal (clj->js opts)) (.modal (if visible "show" "hide")))
      :component-did-update #(let [{:keys [visible]} (r/props %)]
                               (-> % r/dom-node js/$ (.modal (if visible "show" "hide"))))
      :reagent-render (fn [{:keys [class]} & children]
                        (into [:div.ui.modal {:class class}] children))})))
