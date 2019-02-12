(ns shevek.components.popup
  (:require [reagent.core :as r]))

(def popup-data (r/atom {:opened? false}))

(defn show-popup [event new-content {:keys [on-close on-toggle class] :as popup-opts}]
  (reset! popup-data {:opened? true
                      :activator (.-currentTarget event)
                      :content new-content
                      :on-close on-close
                      :on-toggle on-toggle
                      :class class
                      :js-opts (dissoc popup-opts :on-close :on-toggle :class)})
  (when on-toggle (on-toggle true)))

(defn close-popup []
  (swap! popup-data assoc :opened? false)
  (let [{:keys [on-close on-toggle]} @popup-data]
    (when on-close (on-close))
    (when on-toggle (on-toggle false))))

(defn- popup* []
  (let [{:keys [content class]} @popup-data]
    [:div.ui.special.popup {:class class}
     (when content
       (if (fn? content) [content] content))]))

(defn- component-did-update []
  (let [{:keys [opened? activator js-opts]} @popup-data
        activator (-> activator r/dom-node js/$)]
    (if opened?
      (-> activator
          (.popup (clj->js (assoc js-opts
                                  :on "manual" :target activator
                                  :popup ".special.popup" :movePopup false)))
          (.popup "show"))
      (.popup activator "hide"))))

(defn popup []
  (let [node-listener (atom nil)
        handle-click-outside (fn [container event]
                               (when (and (@popup-data :opened?)
                                          (not (.contains (r/dom-node container) (.-target event))))
                                 (when (and (.contains (@popup-data :activator) (.-target event))
                                            (not (-> event .-target js/$ (.is ".close.icon"))))
                                   ; Without this a click on the same activator would close the popup first as is considered outside and then it would trigger the show-popup again
                                   (.stopPropagation event))
                                 (close-popup)))]
    (r/create-class {:reagent-render popup*
                     :component-did-update component-did-update
                     :component-did-mount (fn [container]
                                            (reset! node-listener (partial handle-click-outside container))
                                            (.addEventListener js/document "click" @node-listener true))
                     :component-will-unmount #(.removeEventListener js/document "click" @node-listener true)})))

(defn tooltip [content & [{:as opts}]]
  (let [opts (->> opts
                  (merge {:html content :variation "small inverted"
                          :position "top center" :prefer "opposite"
                          :delay {:show 250}})
                  clj->js)]
    (fn [node]
      (when node (-> node js/$ (.popup opts))))))
