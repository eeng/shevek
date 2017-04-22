(ns shevek.viewer.shared
  (:require-macros [reflow.macros :refer [defevh]])
  (:require [reflow.core :refer [dispatch]]
            [reflow.db :as db]
            [reagent.core :as r]
            [shevek.lib.dates :refer [format-time-according-to-period to-iso8601]]
            [shevek.lib.number :as num]
            [shevek.lib.util :refer [debounce regex-escape]]
            [shevek.i18n :refer [t]]
            [shevek.rpc :as rpc]
            [shevek.dw :as dw]
            [shevek.schemas.conversion :refer [viewer->query]]
            [shevek.components :refer [focused kb-shortcuts]]
            [shevek.reports.url :refer [store-in-url]]
            [schema.core :as s]
            [goog.string :as str]))

(defn- viewer [& keys]
  (db/get-in (into [:viewer] keys)))

(defn current-cube-name []
  (viewer :cube :name))

(defn current-cube [& keys]
  (-> (viewer :cube)
      (get-in keys)))

; Copio el split a arrived-split asi sólo se rerenderiza la table cuando llegan los resultados. Sino se re-renderizaría dos veces, primero inmediatamente luego de splitear y despues cuando llegan los resultados, provocando un pantallazo molesto.
(defevh :query-executed [db results results-keys]
  (-> (assoc-in db (into [:viewer] results-keys) results)
      (assoc-in [:viewer :arrived-split] (-> db :viewer :split))
      (rpc/loaded results-keys)))

; TODO repetida la conversion de viewer a query en schemas.conversion
(defn send-query [db viewer results-keys]
  (store-in-url viewer)
  (let [q (viewer->query viewer)]
    (console.log "Sending query" q) ; TODO no me convence loggear con esto, no habria que usar el logger mas sofisticado? Tb en el interceptor logger
    (rpc/call "querying.api/query" :args [q] :handler #(dispatch :query-executed % results-keys))
    (rpc/loading db results-keys)))

(defn- send-main-query [{:keys [viewer] :as db}]
  (send-query db (assoc viewer :totals true) [:results :main]))

(defn format-measure [{:keys [name type format]} result]
  (let [value (or (->> name keyword (get result)) 0)
        value (condp = type
                "doubleSum" (str/format "%.2f" value)
                "hyperUnique" (str/format "%.0f" value)
                value)]
    (cond-> value
            format (num/format format))))

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
   (when (seq actions) (into [:div.actions] actions))])

(defn- filter-matching [search get-value results]
  (if (seq search)
    (let [pattern (re-pattern (str "(?i)" (regex-escape search)))]
      (filter #(re-find pattern (get-value %)) results))
    results))

(defn- search-button [searching]
  [:i.search.link.icon {:on-click #(swap! searching not)}])

(defn- search-input* [search {:keys [on-change on-stop] :or {on-change identity on-stop identity}}]
  (let [change #(on-change (reset! search %))
        clear #(do (when (seq @search) (change ""))
                 (on-stop))]
     [:div.ui.icon.small.fluid.input.search {:ref (kb-shortcuts :enter on-stop :escape clear)}
      [:input {:type "text" :placeholder (t :input/search) :value @search
               :on-change #(change (.-target.value %))}]
      (if (seq @search)
        [:i.link.remove.circle.icon {:on-click clear}]
        [:i.search.icon])]))

(defn search-input [& args]
  (into [focused search-input*] args))

(defn- highlight [value search]
  (if (seq search)
    (let [[_ pre bold post] (re-find (re-pattern (str "(?i)(.*?)(" (regex-escape search) ")(.*)")) value)]
      [:div.segment-value pre [:span.bold bold] post])
    [:div.segment-value value]))

(def debounce-dispatch (debounce dispatch 500))
