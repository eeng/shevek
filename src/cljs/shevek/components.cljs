; TODO mover esto dentro de la carpeta components y quizas separar en varios.
(ns shevek.components
  (:require [reagent.core :as r :refer [dom-node create-class]]
            [shevek.i18n :refer [t]]
            [shevek.lib.collections :refer [detect wrap-coll]]
            [shevek.lib.react :refer [with-react-keys]]
            [shevek.rpc :refer [loading?]]
            [cuerdas.core :as str]))

(defn page-title [title subtitle icon-class]
  [:h1.ui.header
   [:i.icon {:class icon-class}]
   [:div.content title
    [:div.sub.header subtitle]]])

(defn- classes [& css-classes]
  (->> css-classes (filter identity) (str/join " ")))

(defn- name-from-field-path [path]
  (let [to-name #(if (keyword? %) (name %) (str %))]
    (str/join "-" (map to-name path))))

(defn text-input [atom field & [{:as opts}]]
  (let [path (wrap-coll field)
        opts (merge {:type "text"
                     :value (get-in @atom path)
                     :on-change #(swap! atom assoc-in path (.-target.value %))
                     :name (name-from-field-path path)}
                    opts)]
    [:input opts]))

(defn textarea [atom field & [{:as opts}]]
  (let [path (wrap-coll field)
        opts (merge {:value (get-in @atom path)
                     :on-change #(swap! atom assoc-in path (.-target.value %))
                     :name (name-from-field-path path)}
                    opts)]
    [:textarea opts]))

(defn- checkbox-input [atom field & [{:keys [label class] :as opts}]]
  (let [path (wrap-coll field)
        id (str "cb-" (str/join "-" (map str path)))
        opts (merge {:type "checkbox"
                     :checked (get-in @atom path)
                     :on-change #(swap! atom update-in path not)
                     :id id}
                    opts)]
    [:div.ui.checkbox {:class class}
     [:input opts]
     [:label {:for id} label]]))

(def input-types {:text text-input :textarea textarea :checkbox checkbox-input})

(defn input-field [atom field {:keys [label class input-class as] :or {as :text} :as opts}]
  (let [path (wrap-coll field)
        errors (get-in @atom (into [:errors] path))
        input-opts (cond-> (assoc opts :class input-class)
                           true (dissoc :input-class :as)
                           (not= as :checkbox) (dissoc :label))
        input (input-types as)]
    (assert input (str "Input type '" as "' not supported"))
    [:div.field {:class (classes class (when errors "error"))}
     (when (and label (not= as :checkbox)) [:label label])
     [input atom field input-opts]
     (when errors [:div.ui.pointing.red.basic.label (str/join ", " errors)])]))

; El selected-title es necesario xq semantic muestra la opción seleccionada en el on-change nomás, y en el mount inicial sólo si selected no es nil. En el pinboard measure por ej. el selected arranca en nil y luego cuando llega la metadata se updatea con el selected, pero no se reflejaba en el dropdown xq ya se había ejecutado el $(..).dropdown() antes.
(defn- dropdown [coll {:keys [placeholder selected class on-change] :or {on-change identity}} & content]
  (let [bind-events #(when % (-> % dom-node js/$ (.dropdown #js {:onChange on-change})))]
    [:div.ui.dropdown {:class class :ref bind-events}
     [:input {:type "hidden" :value (or selected "")}]
     (with-react-keys (if (seq content)
                        content
                        [[:div.text (first (detect #(= selected (second %)) coll))]
                         [:i.dropdown.icon]]))
     [:div.menu
      (for [[title val] coll]
        ^{:key val} [:div.item {:data-value val} title])]]))

(defn select [coll {:keys [placeholder] :as opts}]
  [dropdown coll (merge {:class "selection"} opts)
   [:i.dropdown.icon]
   [:div.default.text placeholder]])

(defn- checkbox [id label & [{:keys [checked on-change] :or {on-change identity}}]]
  [:div.ui.checkbox
   [:input {:type "checkbox" :id id :checked (or checked false)
            :on-change #(on-change (not checked))}]
   [:label {:for id} label]])

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

; TODO ver si el auto-focus true (ver login) no soluciona ya esto
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

(defn loader [loading-key]
  (when (loading? loading-key)
    [:div.ui.active.inverted.dimmer [:div.ui.loader]]))
