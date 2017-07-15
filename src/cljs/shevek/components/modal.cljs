(ns shevek.components.modal
  (:require [reagent.core :as r]))

(defonce modal-data (r/atom {}))

(defn show-modal [md]
  (reset! modal-data (assoc md :opened? true)))

; :detachable true es decesario xq sino semantic mueve el dom-node la primera vez entonces la prox vez que se monta se vuelve a recrear
; :observeChanges true es necesario para que se centre bien verticalmente cuando cambia el contenido
(defn- bind-modal-events [dom-node]
  (when dom-node
    (-> dom-node js/$
        (.modal #js {:detachable false
                     :observeChanges true
                     :onHidden (fn [] (swap! modal-data {:opened? false}))})
        (.modal "show"))))

(defn modal []
  (let [{:keys [opened? class header content actions]} @modal-data]
    (when opened?
      [:div.ui.modal {:ref bind-modal-events :class class}
       (if (vector? header)
         header
         [:div.header header])
       [:div.content content]
       (when actions
         (into [:div.actions] actions))])))
