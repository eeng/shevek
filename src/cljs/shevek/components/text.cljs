(ns shevek.components.text
  (:require [shevek.rpc :refer [loading?]]))

(defn mail-to [address]
  (when (seq address)
    [:a {:href (str "mailto:" address)} address]))

(defn loader [loading-key & [{:as opts}]]
  [:div.ui.inverted.dimmer {:class (when (loading? loading-key) "active")}
   [:div.ui.loader opts]])
