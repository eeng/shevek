(ns pivot.components
  (:require [reagent.core :as r :refer [dom-node create-class]]
            [pivot.i18n :refer [t]]
            [pivot.lib.collections :refer [detect]]
            [pivot.lib.react :refer [with-react-keys]]
            [cuerdas.core :as str]))

(defn page-title [title subtitle icon-class]
  [:h1.ui.header
   [:i.icon {:class icon-class}]
   [:div.content title
    [:div.sub.header subtitle]]])

(defn- dropdown* [coll {:keys [placeholder selected class]} & content]
  [:div.ui.dropdown {:class class}
   [:input {:type "hidden" :value (or selected "")}]
   (with-react-keys content)
   [:div.menu
    (for [[title val] coll]
      ^{:key val} [:div.item {:data-value val} title])]])

(defn make-dropdown [{:keys [on-change class] :or {on-change identity}} content]
  (let [bind-events #(-> % dom-node js/$
                         (.dropdown #js {:onChange on-change}))]
    (create-class {:reagent-render content
                   :component-did-mount bind-events})))

; El selected-title es necesario xq semantic muestra la opción seleccionada en el on-change nomás, y en el mount inicial sólo si selected no es nil. En el pinboard measure por ej. el selected arranca en nil y luego cuando llega la metadata se updatea con el selected, pero no se reflejaba en el dropdown xq ya se había ejecutado el $(..).dropdown() antes.
(defn dropdown [_ opts & _]
  (make-dropdown opts (fn [coll {:keys [selected] :as opts} & content]
                        (if (seq content)
                          (into [dropdown* coll opts] content)
                          (let [selected-title (first (detect #(= selected (second %)) coll))]
                            [dropdown* coll opts
                             [:div.text selected-title]
                             [:i.dropdown.icon]])))))

(defn select [_ opts]
  (make-dropdown opts (fn [coll {:keys [placeholder] :as opts}]
                        [dropdown* coll (assoc opts :class "selection")
                         [:i.dropdown.icon]
                         [:div.default.text placeholder]])))

(defn- checkbox [label & [{:keys [checked on-change name]
                           :or {on-change identity name (str/slug label)}}]]
  [:div.ui.checkbox
   [:input {:type "checkbox" :id name :checked (or checked false)
            :on-change #(on-change (not checked))}]
   [:label {:for name} label]])

(defn- popup* [activator popup-container _]
  [:div activator popup-container])

; TODO hacer solo el reposition si on = manual, asi para los demas casos sigue normal
(defn popup [_ _ opts]
  (create-class {:reagent-render popup*
                 :component-did-mount #(-> % dom-node js/$ (.find ".item")
                                           (.popup (clj->js (merge {:inline true} opts)))
                                           (.popup "reposition"))}))
