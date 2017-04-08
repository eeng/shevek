(ns shevek.viewer.shared
  (:require-macros [reflow.macros :refer [defevh]])
  (:require [reflow.core :refer [dispatch]]
            [reflow.db :as db]
            [reagent.core :as r]
            [shevek.lib.dates :refer [format-time-according-to-period to-iso8601]]
            [shevek.lib.util :refer [debounce regex-escape]]
            [shevek.i18n :refer [t]]
            [shevek.rpc :as rpc]
            [shevek.dw :as dw]
            [shevek.schemas.query :refer [Query]]
            [shevek.components :refer [focused]]
            [schema.core :as s]
            [schema-tools.core :as st]
            [com.rpl.specter :refer [setval ALL]]
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

; Convierto manualmente los goog.dates en el intervalo a iso8601 strings porque sino explota transit xq no los reconoce. Alternativamente se podría hacer un handler de transit pero tendría que manejarme con dates en el server y por ahora usa los strings que devuelve Druid nomas.
(defn- add-interval [{:keys [filter] :as q} max-time]
  (let [period (:selected-period (dw/time-dimension filter))]
    (setval [:filter ALL dw/time-dimension? :interval]
            (mapv to-iso8601 (dw/to-interval period max-time))
            q)))

(defn send-query [db q results-keys]
  (let [cube (get-in db [:viewer :cube])
        q (-> (add-interval q (cube :max-time))
              (assoc :cube (cube :name))
              (st/select-schema Query))]
    (console.log "Sending query" q) ; TODO no me convence loggear con esto, no habria que usar el logger mas sofisticado? Tb en el interceptor logger
    (rpc/call "querying.api/query" :args [q] :handler #(dispatch :query-executed % results-keys))
    (rpc/loading db results-keys)))

(defn- send-main-query [{:keys [viewer] :as db}]
  (let [q (assoc viewer :totals true)]
    (send-query db q [:results :main])))

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

(defn search-input [& args]
  (into [focused search-input*] args))

(defn- highlight [value search]
  (if (seq search)
    (let [[_ pre bold post] (re-find (re-pattern (str "(?i)(.*?)(" (regex-escape search) ")(.*)")) value)]
      [:div.segment-value pre [:span.bold bold] post])
    [:div.segment-value value]))

(def debounce-dispatch (debounce dispatch 500))

(defn clean-dim [dim]
  (select-keys dim [:name :title :type]))
