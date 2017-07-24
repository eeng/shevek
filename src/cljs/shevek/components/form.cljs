(ns shevek.components.form
  (:require [reagent.core :as r :refer [dom-node create-class]]
            [shevek.lib.collections :refer [detect wrap-coll]]
            [shevek.lib.react :refer [with-react-keys]]
            [cuerdas.core :as str]
            [shevek.i18n :refer [t]]))

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

(def Keys {13 :enter 27 :escape})

(defn- handle-keypressed [shortcuts e]
  (let [key (-> e .-which Keys)
        assigned-fn (shortcuts key)]
    (when assigned-fn (assigned-fn))))

(defn kb-shortcuts [& {:as shortcuts}]
  (fn [dom-node]
    (when dom-node
      (-> dom-node js/$ (.on "keyup" (partial handle-keypressed shortcuts))))))

(defn hold-to-confirm [holding f & {:keys [seconds-to-confirm i18n-title-key]
                                    :or {seconds-to-confirm 2 i18n-title-key :actions/hold-delete}}]
  (let [timeout #(when @holding (f))]
    {:on-mouse-down #(reset! holding (js/setTimeout timeout (* seconds-to-confirm 1000)))
     :on-mouse-up #(swap! holding js/clearTimeout)
     :on-click #(.stopPropagation %)
     :class (when @holding "holding")
     :title (t i18n-title-key seconds-to-confirm)}))
