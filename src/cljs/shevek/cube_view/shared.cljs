(ns shevek.cube-view.shared
  (:require-macros [reflow.macros :refer [defevh]])
  (:require [reflow.core :refer [dispatch]]
            [reflow.db :as db]
            [reagent.core :as r]
            [shevek.lib.react :refer [with-react-keys]]
            [shevek.lib.dates :refer [format-time-according-to-period]]
            [shevek.lib.util :refer [debounce regex-escape]]
            [shevek.i18n :refer [t]]
            [shevek.rpc :as rpc]
            [shevek.dw :as dw]
            [goog.string :as str]))

(defn- cube-view [& keys]
  (db/get-in (into [:cube-view] keys)))

(defn current-cube-name []
  (cube-view :cube))

(defn current-cube [cube-key]
  (-> (db/get :cubes)
      (get (current-cube-name))
      cube-key))

; Copio el split a split-arrived asi sólo se rerenderiza la table cuando llegan los resultados. Sino se re-renderizaría dos veces, primero inmediatamente luego de splitear y despues cuando llegan los resultados, provocando un pantallazo molesto.
(defevh :query-executed [db results results-keys]
  (-> (assoc-in db (into [:cube-view] results-keys) results)
      (assoc-in [:cube-view :split-arrived] (-> db :cube-view :split))
      (rpc/loaded results-keys)))

(defn send-query [db {:keys [measures] :as cube-view} results-keys]
  (if (seq measures)
    (do
      (rpc/call "dw/query"
                :args [(dw/to-dw-query cube-view)]
                :handler #(dispatch :query-executed % results-keys))
      (rpc/loading db results-keys))
    db))

(defn- send-main-query [{:keys [cube-view] :as db}]
  (send-query db (assoc cube-view :totals true) [:results :main]))

(defn format-measure [{:keys [name type]} result]
  (let [value (or (->> name keyword (get result)) 0)]
    (condp = type
      "doubleSum" (str/format "%.2f" value)
      "hyperUnique" (str/format "%.0f" value)
      value)))

(defn- totals-result? [result dim]
  (not (contains? result (-> dim :name keyword))))

(defn format-dimension [{:keys [granularity name] :as dim} result]
  (let [value (-> name keyword result)]
    (cond
      (totals-result? result dim) "Total"
      (nil? value) "Ø"
      (dw/time-dimension? dim) (format-time-according-to-period value granularity)
      :else value)))

(defn- panel-header [text & actions]
  [:h2.ui.sub.header text
   (when (seq actions) [:div.actions (with-react-keys actions)])])

(defn- filter-matching [search get-value results]
  (if (seq search)
    (filter #(re-find (re-pattern (str "(?i)" (regex-escape search)))
                      (get-value %))
            results)
    results))

(defn- search-button [searching]
  [:i.search.link.icon {:on-click #(swap! searching not)}])

(defn- search-input* [search {:keys [on-change on-stop] :or {on-change identity on-stop identity}}]
  (let [change #(on-change (reset! search %))
        clear #(do (when (seq @search) (change ""))
                 (on-stop))]
    [:div.ui.icon.small.fluid.input.search
     [:input {:type "text" :placeholder (t :input/search) :value @search
              :on-change #(change (.-target.value %))
              :on-key-down #(case (.-which %)
                              13 (on-stop) ; TODO en el enter habria que tildar una opcion
                              27 (clear)
                              nil)}]
     (if (seq @search)
       [:i.link.remove.circle.icon {:on-click clear}]
       [:i.search.icon])]))

; El setTimeout es necesario xq sino no funcaba el focus en el search dentro de filter popups.
(def search-input
  (with-meta search-input*
    {:component-did-mount (fn [rc] (js/setTimeout #(-> rc r/dom-node js/$ (.find "input") .focus) 0))}))

(defn- highlight [value search]
  (if (seq search)
    (let [[_ pre bold post] (re-find (re-pattern (str "(?i)(.*?)(" (regex-escape search) ")(.*)")) value)]
      [:div.segment-value pre [:span.bold bold] post])
    [:div.segment-value value]))

(def debounce-dispatch (debounce dispatch 500))
