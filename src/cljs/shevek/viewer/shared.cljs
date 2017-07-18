(ns shevek.viewer.shared
  (:require-macros [shevek.reflow.macros :refer [defevh]])
  (:require [shevek.reflow.core :refer [dispatch]]
            [shevek.reflow.db :as db]
            [reagent.core :as r]
            [shevek.lib.dates :refer [format-time-according-to-period to-iso8601]]
            [shevek.lib.number :as num]
            [shevek.lib.util :refer [debounce regex-escape]]
            [shevek.i18n :refer [t]]
            [shevek.rpc :as rpc]
            [shevek.lib.dw.dims :refer [dim= add-dimension remove-dimension time-dimension?]]
            [shevek.schemas.conversion :refer [viewer->query]]
            [shevek.components.form :refer [kb-shortcuts]]
            [shevek.viewer.url :refer [store-viewer-in-url]]
            [schema.core :as s]
            [goog.string :as str]
            [shevek.lib.logger :as log]))

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

(defn send-query [db viewer results-keys]
  (let [q (viewer->query viewer)]
    (log/info "Sending query" q)
    (rpc/call "querying.api/query" :args [q] :handler #(dispatch :query-executed % results-keys))
    (rpc/loading db results-keys)))

(defn send-main-query [{:keys [viewer] :as db}]
  (send-query db (assoc viewer :totals true) [:results :main]))

(defn- remove-dim-unless-time [dim coll]
  (if (time-dimension? dim)
    coll
    (remove-dimension coll dim)))

(defn send-pinned-dim-query [{:keys [viewer] :as db} {:keys [name] :as dim} & [{:as search-filter}]]
  (let [q (cond-> {:cube (:cube viewer)
                   :filter (remove-dim-unless-time dim (:filter viewer))
                   :split [dim]
                   :measures (vector (get-in viewer [:pinboard :measure]))}
                  search-filter (update :filter add-dimension search-filter))]
    (send-query db q [:results :pinboard name])))

(defn send-pinboard-queries
  ([db] (send-pinboard-queries db nil))
  ([db except-dim]
   (->> (get-in db [:viewer :pinboard :dimensions])
        (remove-dim-unless-time except-dim)
        (reduce #(send-pinned-dim-query %1 %2) db))))

(defn dimension-value [{:keys [name]} result]
  (->> name keyword (get result)))

(defn format-measure [{:keys [type format] :as dim} result]
  (let [value (or (dimension-value dim result) 0)
        value (condp = type
                "doubleSum" (str/format "%.2f" value)
                "hyperUnique" (str/format "%.0f" value)
                value)]
    (cond-> value
            format (num/format format))))

(defn- totals-result? [result dim]
  (not (contains? result (-> dim :name keyword))))

(defn format-dim-value [value {:keys [granularity name type] :as dim}]
  (cond
    (nil? value) "Ø"
    (time-dimension? dim) (format-time-according-to-period value granularity)
    (= "BOOL" type) (t (keyword (str "boolean/" value)))
    :else value))

(defn format-dimension [dim result]
  (if (totals-result? result dim)
    "Total"
    (format-dim-value (dimension-value dim result) dim)))

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

(defn search-input [search {:keys [on-change on-stop] :or {on-change identity on-stop identity}}]
  (let [change #(on-change (reset! search %))
        clear #(do (when (seq @search) (change ""))
                 (on-stop))]
     [:div.ui.icon.small.fluid.input.search {:ref (kb-shortcuts :enter on-stop :escape clear)}
      [:input {:type "text" :placeholder (t :input/search) :value @search
               :on-change #(change (.-target.value %)) :auto-focus true}]
      (if (seq @search)
        [:i.link.remove.circle.icon {:on-click clear}]
        [:i.search.icon])]))

(defn- highlight [value search]
  (if (seq search)
    (let [[_ pre bold post] (re-find (re-pattern (str "(?i)(.*?)(" (regex-escape search) ")(.*)")) value)]
      [:div.segment-value pre [:span.bold bold] post])
    [:div.segment-value value]))

(def debounce-dispatch (debounce dispatch 500))
