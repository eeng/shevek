(ns shevek.components.form
  (:require [reagent.core :as r]
            [shevek.lib.collections :refer [detect wrap-coll]]
            [shevek.lib.react :refer [with-react-keys]]
            [shevek.lib.string :refer [regex-escape split]]
            [cuerdas.core :as str]
            [shevek.i18n :refer [t]]
            [shevek.notification :refer [notify]]))

(defn- classes [& css-classes]
  (->> css-classes (filter identity) (str/join " ")))

;; Basic components

(defn- checkbox [id label & [{:keys [checked on-change] :or {on-change identity}}]]
  [:div.ui.checkbox
   [:input {:type "checkbox" :id id :checked (or checked false)
            :on-change #(on-change (not checked))}]
   [:label {:for id} label]])

; El selected-title es necesario xq semantic muestra la opción seleccionada en el on-change nomás, y en el mount inicial sólo si selected no es nil. En el pinboard measure por ej. el selected arranca en nil y luego cuando llega la metadata se updatea con el selected, pero no se reflejaba en el dropdown xq ya se había ejecutado el $(..).dropdown() antes.
(defn- dropdown [coll {:keys [placeholder selected class on-change] :or {on-change identity}} & content]
  (let [bind-events #(when % (-> % r/dom-node js/$ (.dropdown #js {:onChange on-change})))]
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

;; Atom associated components

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
                     :checked (= (get-in @atom path) true) ; without the = true reagent throws a warning when the value is nil
                     :on-change #(swap! atom update-in path not)
                     :id id}
                    opts)]
    [:div.ui.checkbox {:class class}
     [:input opts]
     [:label {:for id} label]]))

(defn- select-multiple-input [atom field & [{:keys [collection] :as opts}]]
  (let [opts (merge {:class "multiple fluid search selection"
                     :selected (get-in @atom field)
                     :on-change #(swap! atom assoc-in field (split % #","))}
                    opts)]
    [select collection opts]))

(def input-types {:text text-input :textarea textarea :checkbox checkbox-input
                  :select-multiple select-multiple-input})

(defn input-field [atom field {input-opts :input :keys [label class wrapper as icon] :or {as :text} :as opts}]
  (let [field-path (wrap-coll field)
        errors (get-in @atom (into [:errors] field-path))
        input (input-types as)
        input-opts (cond-> (merge input-opts (dissoc opts :input :class :wrapper :as :icon))
                           (not= as :checkbox) (dissoc :label))]
    (assert input (str "Input type '" as "' not supported"))
    [:div.field {:class (classes class (when errors "error"))}
     (when (and label (not= as :checkbox)) [:label label])
     [:div.ui.input (merge {:class (when icon "left icon")} wrapper)
      (when icon [:i.icon {:class icon}])
      [input atom field-path input-opts]]
     (when errors [:div.ui.pointing.red.basic.label (str/join ", " errors)])]))

;; Other components

(defn toggle-checkbox-inside [e]
  (-> e .-target js/$ (.find ".checkbox input") .click))

(def Keys {13 :enter 27 :escape})

(defn- handle-keypressed [shortcuts e]
  (let [key (-> e .-which Keys)
        assigned-fn (shortcuts key)
        tag (-> e .-target .-tagName)]
    (when (and assigned-fn (not= "TEXTAREA" tag))
      (assigned-fn))))

(defn kb-shortcuts [& {:as shortcuts}]
  (fn [dom-node]
    (when dom-node
      (-> dom-node js/$ (.on "keyup" (partial handle-keypressed shortcuts))))))

(defn by [& fields]
  #(str/join "|" ((apply juxt fields) %)))

(defn- filter-matching [search get-value results]
  (if (seq search)
    (let [pattern (re-pattern (str "(?i)" (regex-escape search)))]
      (filter #(re-find pattern (get-value %)) results))
    results))

(defn search-input [search {:keys [on-change on-stop on-enter input wrapper]
                            :or {on-change identity on-stop identity on-enter identity input {} wrapper {}}}]
  (let [change #(on-change (reset! search %))
        clear #(do (when (seq @search) (change ""))
                 (on-stop))
        wrapper-opts (merge {:ref (kb-shortcuts :enter on-enter :escape clear)} wrapper)
        input-opts (merge {:type "text" :placeholder (t :input/search) :value @search
                           :on-change #(change (.-target.value %)) :auto-focus true}
                          input)]
     [:div.ui.icon.fluid.input.search wrapper-opts
      [:input input-opts]
      (if (seq @search)
        [:i.link.remove.circle.icon {:on-click clear}]
        [:i.search.icon])]))

(defonce holding (r/atom nil))
(def holding-time 2)

(defn cancel-timeout []
  (when @holding
    (swap! holding js/clearTimeout)
    (notify (t :actions/hold-delete holding-time) :type :info)))

(defonce mouseup-listener
  (do
    (.addEventListener js/document "mouseup" cancel-timeout true)
    true))

(defn start-timeout [action]
  (let [later (fn []
                (action)
                (reset! holding nil))]
    (reset! holding (js/setTimeout later (* holding-time 1000)))))

(defn hold-to-confirm [on-confirm]
  {:on-mouse-down #(start-timeout on-confirm)
   :on-click #(.stopPropagation %)})
