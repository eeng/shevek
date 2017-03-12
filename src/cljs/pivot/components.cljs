(ns pivot.components
  (:require [reagent.core :refer [dom-node create-class]]
            [pivot.i18n :refer [t]]))

(defn page-title [title subtitle icon-class]
  [:h1.ui.header
   [:i.icon {:class icon-class}]
   [:div.content title
    [:div.sub.header subtitle]]])

(defn make-dropdown [{:keys [on-change] :or {on-change identity}} content]
  (let [bind-events #(-> % dom-node js/$
                         (.dropdown #js {:onChange on-change}))]
    (create-class {:reagent-render content
                   :component-did-mount bind-events})))

(defn- dropdown* [coll & [{:keys [placeholder selected] :or {selected ""}}]]
  [:div.ui.selection.dropdown
   [:input {:type "hidden" :value selected}]
   [:i.dropdown.icon]
   [:div.default.text placeholder]
   [:div.menu
    (for [[title val] coll]
      ^{:key val}
      [:div.item {:data-value val} title])]])

(defn dropdown [_ & [opts]]
  (make-dropdown opts dropdown*))
