(ns shevek.notification
  (:require [reagent.core :as r]
            [shevek.lib.util :refer [variable-debounce]]))

(defonce state (r/atom {}))

(def hide-after (variable-debounce #(swap! state assoc :showed? false)))

(defn notify [message & {:keys [timeout] :or {timeout 5000} :as opts}]
  (reset! state (assoc opts :message message :showed? true))
  (when (pos? timeout) (hide-after timeout)))

(defn notification []
  (let [{:keys [showed? message]} @state]
    [:div#notification
     {:class (if showed? "visible" "hidden")}
     [:div.ui.positive.icon.message
      [:i.checkmark.icon]
      [:div.content message]]]))
