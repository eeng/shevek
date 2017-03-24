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
                        [dropdown* coll (merge {:class "selection"} opts)
                         [:i.dropdown.icon]
                         [:div.default.text placeholder]])))

(defn- checkbox [label & [{:keys [checked on-change name]
                           :or {on-change identity name (str/slug label)}}]]
  [:div.ui.checkbox
   [:input {:type "checkbox" :id name :checked (or checked false)
            :on-change #(on-change (not checked))}]
   [:label {:for name} label]])

; TODO el reposition solo se deberia hacer :on "manual"
(defn make-popup [activator popup-opts]
  (with-meta activator
    {:component-did-mount #(-> % dom-node js/$
                               (.popup (clj->js (assoc popup-opts :inline true)))
                               (.popup "reposition"))
     :component-did-update #(-> % dom-node js/$
                                (.popup "reposition"))}))

; TODO quizas se podria dejar de usar el on manual si en el open lo abrimos con js y cerramos con js. Asi se evitaria todo el manejo del click-outside y de paso habria lindas transiciones
(defn controlled-popup [activator popup-content {:keys [on-open] :or {on-open identity} :as opts}]
  (fn [& _]
    (let [popup-opts (select-keys opts [:position])
          opened (r/atom false)
          negate-and-notify #(let [open (not %)]
                               (when open (on-open))
                               open)
          toggle #(swap! opened negate-and-notify)
          close #(reset! opened false)
          handle-click-outside (fn [c e]
                                 (when (and @opened (not (.contains (r/dom-node c) (.-target e))))
                                   (close)))
          node-listener (atom nil)]
      (r/create-class {:reagent-render
                       (fn [& args]
                         (let [popup-object {:opened? @opened :toggle toggle :close close}]
                           [:div
                            (into [(make-popup activator (assoc popup-opts :on "manual")) popup-object] args)
                            (into [popup-content popup-object] args)]))
                       :component-did-mount
                       #(do
                          (reset! node-listener (partial handle-click-outside %))
                          (.addEventListener js/document "click" @node-listener true))
                       :component-will-unmount
                       #(.removeEventListener js/document "click" @node-listener true)}))))
