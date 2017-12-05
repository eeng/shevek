(ns shevek.components.text
  (:require [shevek.rpc :refer [loading?]]))

(defn page-title [title subtitle icon-class]
  [:h1.ui.header
   [:i.icon {:class icon-class}]
   [:div.content title
    [:div.sub.header subtitle]]])

(defn mail-to [address]
  (when (seq address)
    [:a {:href (str "mailto:" address)} address]))

(defn loader [loading-key & [{:as opts}]]
  [:div.ui.inverted.dimmer {:class (when (loading? loading-key) "active")}
   [:div.ui.loader opts]])

(defn warning [text]
  [:div.icon-hint
   [:i.warning.circle.icon]
   [:div.text text]])
