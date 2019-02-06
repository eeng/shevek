(ns shevek.components.modal
  (:require [reagent.core :as r]))

(defonce modal-data (r/atom {}))

(defn show-modal [md]
  (reset! modal-data (assoc md :opened? true)))

(defn close-modal []
  (swap! modal-data assoc :opened? false))

; :detachable true es decesario xq sino semantic mueve el dom-node la primera vez entonces la prox vez que se monta se vuelve a recrear
; :observeChanges true es necesario para que se centre bien verticalmente cuando cambia el contenido
(defn- bind-modal-events [dom-node]
  (if dom-node
    (let [opts (merge {:detachable false
                       :observeChanges true
                       :onHidden #(swap! modal-data {:opened? false})}
                      (:js-opts @modal-data))]
      (-> dom-node js/$ (.modal (clj->js opts)) (.modal "show")))
    (-> ".ui.modal" js/$ (.modal "hide"))))

; TODO DASHBOARD ver de usar siempre la forma con el cuerpo del modal completo
(defn modal []
  (let [{:keys [opened? class header content actions modal]} @modal-data]
    (when opened?
      (if modal
        [:div.ui.modal {:ref bind-modal-events :class class}
         modal]
        [:div.ui.modal {:ref bind-modal-events :class class}
         (if (vector? header) header [:div.header header])
         [:div.content content]
         (when actions (into [:div.actions] actions))]))))
