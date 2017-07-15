(ns shevek.components.popup
  (:require [reagent.core :as r]))

(defn controlled-popup [activator popup-content {:keys [init-open? on-open on-close]
                                                 :or {init-open? false on-open identity on-close identity}
                                                 :as opts}]
  (fn [& _]
    (let [popup-opts (select-keys opts [:position :distanceAway])
          opened (r/atom false)
          toggle (fn [new-val]
                   (when (compare-and-set! opened (not new-val) new-val)
                     (if new-val (on-open) (on-close))))
          open #(toggle true)
          close #(toggle false)
          handle-click-outside (fn [c e]
                                 (when (and @opened (not (.contains (r/dom-node c) (.-target e))))
                                   (close)))
          node-listener (atom nil)
          show-popup #(-> % r/dom-node js/$ (.find "> *:first")
                          (.popup (clj->js (assoc popup-opts :inline true :on "manual")))
                          (.popup "show"))]
      (when init-open? (open))
      (r/create-class
       {:reagent-render
        (fn [& args]
          (let [popup-object {:opened? @opened :toggle #(toggle (not @opened)) :close close}]
            [:div
             (into [activator popup-object] args)
             (when @opened [:div.ui.special.popup (into [popup-content popup-object] args)])]))
        :component-did-mount #(do
                                (when @opened (show-popup %))
                                (reset! node-listener (partial handle-click-outside %))
                                (.addEventListener js/document "click" @node-listener true))
        :component-did-update #(when @opened (show-popup %))
        :component-will-unmount #(.removeEventListener js/document "click" @node-listener true)}))))

(defonce popup-data (r/atom {}))

(defn toggle-popup [event new-content popup-opts]
  (let [{:keys [opened? content]} @popup-data
        new-popup (if (and opened? (= content new-content))
                    {}
                    {:opened? true :content new-content :activator (.-target event) :opts popup-opts})]
    (reset! popup-data new-popup)))

(defn- popup* []
  (let [{:keys [opened? content]} @popup-data]
    (when opened?
      [:div.ui.special.popup [content]])))

(defn- component-did-update []
  (let [{:keys [opened? activator opts]} @popup-data
        activator (-> activator r/dom-node js/$)]
    (if opened?
      (-> activator
          (.popup (clj->js (assoc (@popup-data :opts)
                                  :inline true :on "manual" :target activator
                                  :popup ".special.popup" :movePopup false)))
          (.popup "show"))
      (.popup activator "destroy"))))

(defn popup []
  (r/create-class {:reagent-render popup* :component-did-update component-did-update}))
