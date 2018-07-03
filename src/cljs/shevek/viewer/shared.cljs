(ns shevek.viewer.shared
  (:require [shevek.reflow.core :refer [dispatch] :refer-macros [defevh]]
            [shevek.reflow.db :as db]
            [reagent.core :as r]
            [shevek.lib.time.ext :refer [format-interval format-time-according-to-period]]
            [shevek.lib.number :as num]
            [shevek.lib.util :refer [debounce]]
            [shevek.i18n :refer [t translation]]
            [shevek.rpc :as rpc]
            [shevek.lib.dw.dims :refer [dim= add-dimension remove-dimension time-dimension?]]
            [shevek.schemas.conversion :refer [viewer->query]]
            [shevek.components.form :refer [kb-shortcuts]]
            [shevek.components.popup :refer [tooltip]]
            [shevek.viewer.url :refer [store-viewer-in-url]]
            [schema.core :as s]
            [goog.string :as gstr]
            [cuerdas.core :as str]
            [shevek.lib.logger :as log]
            [shevek.lib.string :refer [format-bool regex-escape]]
            [shevek.menu.settings :refer [restart-auto-refresh!]]))

(defn viewer [& keys]
  (db/get-in (into [:viewer] keys)))

(defn current-cube-name []
  (viewer :cube :name))

(defn current-cube [& keys]
  (-> (viewer :cube)
      (get-in keys)))

(defevh :viewer/query-executed [db results results-keys]
  (-> (assoc-in db results-keys results)
      (rpc/loaded results-keys)))

(defn send-query [db viewer results-keys]
  (let [q (viewer->query viewer)
        results-keys (into [:viewer :results] results-keys)]
    (log/info "Sending query" q)
    (rpc/call "querying/query" :args [q] :handler #(dispatch :viewer/query-executed % results-keys))
    (rpc/loading db results-keys)))

(defevh :visualization/query-executed [db results results-keys viewer]
  (restart-auto-refresh!)
  (let [viz (-> (select-keys viewer [:viztype :splits :measures])
                (assoc :results results))]
    (-> (assoc-in db results-keys viz)
        (rpc/loaded results-keys))))

(defn send-visualization-query [db viewer results-keys]
  (let [q (viewer->query (assoc viewer :totals true))]
    (log/info "Sending visualization query" q)
    (rpc/call "querying/query" :args [q] :handler #(dispatch :visualization/query-executed % results-keys viewer))
    (rpc/loading db results-keys)))

(defn send-main-query [{:keys [viewer] :as db}]
  (send-visualization-query db viewer [:viewer :visualization]))

(defn- remove-dim-unless-time [dim coll]
  (if (time-dimension? dim)
    coll
    (remove-dimension coll dim)))

(defn send-pinned-dim-query [{:keys [viewer] :as db} {:keys [name] :as dim} & [{:as search-filter}]]
  (let [q (cond-> {:cube (:cube viewer)
                   :filters (remove-dim-unless-time dim (:filters viewer))
                   :splits [dim]
                   :measures (vector (get-in viewer [:pinboard :measure]))}
                  search-filter (update :filters add-dimension search-filter))]
    (send-query db q [:pinboard name])))

(defn send-pinboard-queries
  ([db] (send-pinboard-queries db nil))
  ([db except-dim]
   (->> (get-in db [:viewer :pinboard :dimensions])
        (remove-dim-unless-time except-dim)
        (reduce #(send-pinned-dim-query %1 %2) db))))

(defn dimension-value [{:keys [name]} result]
  (->> name keyword (get result)))

(def measure-value dimension-value)

(defn- personalize-abbreviations [format]
  (let [abbreviations (db/get-in [:settings :abbreviations])]
    (case abbreviations
      "yes" (str/replace format #"0$" "0a")
      "no" (str/replace format "a" "")
      format)))

(defn format-measure [{:keys [type format] :as dim} result]
  (when-let [value (measure-value dim result)]
    (if format
      (num/format value (personalize-abbreviations format))
      (condp = type
        "doubleSum" (gstr/format "%.2f" value)
        "hyperUnique" (gstr/format "%.0f" value)
        value))))

(defn totals-result? [result dim]
  (not (contains? result (-> dim :name keyword))))

(defn format-dim-value [value {:keys [granularity name type] :as dim}]
  (cond
    (nil? value) "Ø"
    (time-dimension? dim) (format-time-according-to-period value granularity)
    (= "BOOL" type) (format-bool value)
    :else value))

(defn format-dimension [dim result]
  (when result
    (if (totals-result? result dim)
      "Total"
      (format-dim-value (dimension-value dim result) dim))))

(defn- panel-header [text & actions]
  [:h2.ui.sub.header text
   (when (seq actions) (into [:div.actions] actions))])

(defn- search-button [searching]
  [:i.search.link.icon {:on-click #(swap! searching not)}])

(defn- highlight [value search]
  (if (seq search)
    (let [[_ pre bold post] (re-find (re-pattern (str "(?i)(.*?)(" (regex-escape search) ")(.*)")) value)]
      [:div.segment-value pre [:span.bold bold] post])
    [:div.segment-value value]))

(def debounce-dispatch (debounce dispatch 500))

(defn cube-authorized? [{:keys [measures]}]
  (seq measures))

(defn description-help-icon [{:keys [description]}]
  (when (seq description)
    [:i.help.circle.outline.icon {:ref (tooltip description {:position "right center"})}]))
