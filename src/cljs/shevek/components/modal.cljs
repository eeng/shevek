(ns shevek.components.modal
  (:require [reagent.core :as r]))

(defonce modal-data (r/atom {}))

(defn show-modal [content & [opts]]
  (reset! modal-data {:opened? true :content content :js-opts opts}))

(defn close-modal []
  (swap! modal-data assoc :opened? false))

; :detachable true is necessary because otherwise semantic moves the node so the next time it mounts it gets recreated
; :observeChanges true is for the modal to recenter when content changes
(defn- modal-content [content]
  (r/create-class
   {:display-name "modal"
    :reagent-render (fn [this] content)
    :component-did-mount (fn [this]
                           (js/console.log (:js-opts @modal-data))
                           (let [opts (merge {:detachable false
                                              :onHidden #(swap! modal-data {:opened? false})}
                                             (:js-opts @modal-data))]
                             (-> this r/dom-node js/$ (.modal (clj->js opts)) (.modal "show"))))
    :component-will-unmount (fn [this]
                              (-> ".ui.modal" js/$ (.modal "hide")))}))

(defn modal []
  (let [{:keys [opened? content]} @modal-data]
    (when opened?
      [modal-content content])))
