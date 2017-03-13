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

(defn- checkbox* [label & _]
  [:div.ui.checkbox
   [:input {:type "checkbox"}]
   [:label label]])

(defn checkbox [_ & [{:keys [on-change] :or {on-change identity}}]]
  (create-class {:reagent-render checkbox*
                 :component-did-mount #(-> % dom-node js/$ (.checkbox (clj->js {:onChange on-change})))}))

(defn- popup* [activator popup-container _]
  [:div activator popup-container])

; TODO hacer solo el reposition si on = manual, asi para los demas casos sigue normal
(defn popup [_ _ opts]
  (create-class {:reagent-render popup*
                 :component-did-mount #(-> % dom-node js/$ (.find ".item")
                                           (.popup (clj->js (merge {:inline true} opts)))
                                           (.popup "reposition"))}))
