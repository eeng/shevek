(ns shevek.components.modal2
  (:require [reagent.core :as r]))

(defn modal [props & children]
  (r/create-class
   {:display-name "modal"

    :component-did-mount
    (fn [this]
      (let [{:keys [on-hide] :as props} (r/props this)
            opts (merge {:detachable false
                         :observeChanges true
                         :onHide #(do (on-hide) true)}
                        (dissoc props :class :on-hidden))]
        (-> this r/dom-node js/$
            (.modal (clj->js opts))
            (.modal "show"))))

    :component-will-unmount
    (fn [this]
      (-> this r/dom-node js/$ (.modal "hide")))

    :reagent-render
    (fn [{:keys [class]} & children]
      (into [:div.ui.modal {:class class}]
            children))}))
