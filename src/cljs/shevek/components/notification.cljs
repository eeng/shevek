(ns shevek.components.notification
  (:require [reagent.core :as r]
            [shevek.lib.util :refer [variable-debounce]]))

(defonce state (r/atom {}))

(def hide-after (variable-debounce #(swap! state assoc :showed? false)))

(defn notify [message & {:keys [timeout type] :or {timeout 3000 type :success} :as opts}]
  (reset! state (assoc opts :message message :showed? true))
  (when (pos? timeout) (hide-after timeout)))

(defn- type->class [type]
  (case type
    :warn "warning"
    :info "info"
    :error "negative"
    "positive"))

(defn- type->icon [type]
  (case type
    :warn "warning circle"
    :info "info circle"
    :error "thumbs down"
    "thumbs up"))

(defn notification []
  (let [{:keys [showed? message type]} @state]
    [:div#notification
     {:class (if showed? "visible" "hidden")}
     [:div.ui.icon.message {:class (type->class type)}
      [:i.icon {:class (type->icon type)}]
      [:div.content message]]]))
