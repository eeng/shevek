(ns shevek.components
  (:require [reagent.core :as r :refer [dom-node create-class]]
            [shevek.i18n :refer [t]]
            [shevek.lib.collections :refer [detect]]
            [shevek.lib.react :refer [with-react-keys]]
            [cuerdas.core :as str]))

(defn page-title [title subtitle icon-class]
  [:h1.ui.header
   [:i.icon {:class icon-class}]
   [:div.content title
    [:div.sub.header subtitle]]])

(defn input-text [atom field & [{:as opts}]]
  (let [path (-> field vector flatten)
        opts (merge {:type "text"
                     :value (get-in @atom path)
                     :on-change #(swap! atom assoc-in path (.-target.value %))}
                    opts)]
    [:input opts]))

(defn input-field [atom field {:keys [label class]}]
  [:div.field {:class class}
   [:label label]
   [input-text atom field]])

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

(defn toggle-checkbox-inside [e]
  (-> e .-target js/$ (.find ".checkbox input") .click))

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
          show-popup #(-> % dom-node js/$ (.find "> *:first")
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

(def Keys {13 :enter 27 :escape})

(defn- handle-keypressed [shortcuts e]
  (let [key (-> e .-which Keys)
        assigned-fn (shortcuts key)]
    (when assigned-fn (assigned-fn))))

(defn kb-shortcuts [& {:as shortcuts}]
  (fn [dom-node]
    (when dom-node
      (-> dom-node js/$ (.on "keyup" (partial handle-keypressed shortcuts))))))

; El setTimeout es necesario xq sino no funcaba el focus en el search dentro de filter popups.
(defn focused [& target]
  (r/create-class
   {:reagent-render #(vec target)
    :component-did-mount (fn [rc]
                           (js/setTimeout #(-> rc r/dom-node js/$
                                               (.find "input") (.addBack "input")
                                               .focus .select)
                                          0))}))

(defn mail-to [address]
  (when (seq address)
    [:a {:href (str "mailto:" address)} address]))

(defonce modal-data (r/atom {}))

(defn show-modal [md]
  (reset! modal-data (assoc md :opened? true)))

; :detachable true es decesario xq sino semantic mueve el dom-node la primera vez entonces la prox vez que se monta se vuelve a recrear
; :observeChanges true es necesario para que se centre bien verticalmente cuando cambia el contenido
(defn- bind-modal-events [dom-node]
  (when dom-node
    (-> dom-node js/$
        (.modal #js {:detachable false
                     :observeChanges true
                     :onHidden (fn [] (swap! modal-data {:opened? false}))})
        (.modal "show"))))

(defn- modal []
  (let [{:keys [opened? class header content actions]} @modal-data]
    (when opened?
      [:div.ui.modal {:ref bind-modal-events :class class}
       (if (vector? header)
         header
         [:div.header header])
       [:div.content content]
       (when actions
         [:div.actions actions])])))
