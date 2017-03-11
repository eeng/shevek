(ns pivot.components
  (:require [reagent.core :refer [dom-node create-class]]
            [pivot.i18n :refer [t]]))

(defn page-title [title subtitle icon-class]
  [:h1.ui.header
   [:i.icon {:class icon-class}]
   [:div.content title
    [:div.sub.header subtitle]]])

(defn- dropdown* [coll & [{:keys [placeholder]}]]
  [:div.ui.selection.dropdown
   [:input {:type "hidden"}]
   [:i.dropdown.icon]
   [:div.default.text placeholder]
   [:div.menu
    (for [[title val] coll]
      ^{:key val}
      [:div.item {:data-value val} title])]])

(defn dropdown [_ & [{:keys [on-change selected] :or {on-change identity selected ""}}]]
  (let [bind-events #(-> % dom-node js/$
                         (.dropdown #js {:onChange on-change})
                         (.dropdown "set selected" selected))]
    (create-class {:reagent-render dropdown*
                   :component-did-mount bind-events})))
