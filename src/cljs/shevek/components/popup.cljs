(ns shevek.components.popup
  (:require [reagent.core :as r]))

(def popup-data (r/atom {:opened? false}))

(defn show-popup [event new-content {:keys [on-close id class] :or {on-close identity} :as popup-opts}]
  (let [{:keys [opened? content activator]} @popup-data]
    (reset! popup-data {:opened? true :activator (.-currentTarget event) :content new-content
                        :on-close on-close :id id :class class
                        :js-opts (dissoc popup-opts :on-close :id :class)})))

(defn close-popup []
  (swap! popup-data assoc :opened? false)
  (let [{:keys [on-close]} @popup-data]
    (when on-close (on-close))))

(defn popup-opened? [id]
  (and (@popup-data :opened?) (= id (@popup-data :id))))

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

(defn tooltip [content]
  (fn [node]
    (when node
      (-> node js/$ (.popup #js {:content content :variation "small inverted"
                                 :position "top center" :prefer "opposite"})))))
