(ns pivot.cube-view.shared
  (:require-macros [reflow.macros :refer [defevh]])
  (:require [reflow.core :refer [dispatch]]
            [reflow.db :as db]
            [reflow.db :as db]
            [pivot.lib.react :refer [with-react-keys]]
            [pivot.lib.dates :refer [format-time-according-to-period]]
            [pivot.i18n :refer [t]]
            [pivot.rpc :as rpc]
            [pivot.dw :as dw]
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
