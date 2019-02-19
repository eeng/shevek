(ns shevek.pages.designer.helpers
  (:require [shevek.reflow.core :refer [dispatch] :refer-macros [defevh]]
            [shevek.reflow.db :as db]
            [shevek.components.popup :refer [tooltip]]
            [shevek.components.refresh :refer [debounce-auto-refresh!]]
            [shevek.lib.string :refer [regex-escape]]
            [shevek.lib.logger :as log]
            [shevek.lib.util :refer [debounce]]
            [shevek.domain.dimension :refer [dim= add-dimension remove-dimension time-dimension?]]
            [shevek.pages.cubes.helpers :refer [get-cube]]
            [shevek.rpc :as rpc]
            [shevek.schemas.conversion :refer [designer->report report->designer unexpand]]
            [shevek.i18n :refer [t]]))

(defn current-cube [& keys]
  (let [cube-name (db/get-in [:designer :report :cube])
        cube (get-cube cube-name)]
    (get-in cube keys)))

(defn- store-report-changes [{{:keys [on-report-change]} :designer :as db} report]
  (let [db (assoc-in db [:designer :report] report)]
    (on-report-change report)
    db))

(defn- report->query [report]
  (-> report
      (select-keys [:cube :measures :filters :splits])
      (assoc :totals true)))

(defevh :designer/results-arrived [db results results-path pending-report]
  (when pending-report
    (debounce-auto-refresh!))
  (-> (assoc-in db results-path results)
      (rpc/loaded results-path)
      (cond-> ; We change the report only after results arrived so the visualization doesn't rerender until that moment
              pending-report (store-report-changes pending-report))))

(defn- send-query [db query results-path & [pending-report]]
  (log/info "Sending query" query)
  (rpc/call "querying/query" :args [query] :handler #(dispatch :designer/results-arrived % results-path pending-report))
  (rpc/loading db results-path))

(defn send-report-query [db report results-path]
   (send-query db (report->query report) results-path))

(defn notify-designer-changes [{:keys [designer] :as db}]
  (store-report-changes db (designer->report designer)))

(defn send-designer-query [{:keys [designer] :as db}]
  (let [report (designer->report designer)]
    (send-query db (report->query report) [:designer :report-results] report)))

(defn- remove-dim-unless-time [dim coll]
  (if (time-dimension? dim)
    coll
    (remove-dimension coll dim)))

; TODO DASHBOARD esto del unexpand y tantos metodos de conversion no me convence, revisar. Tambien ver si no conviene manejarse con algo mas simple como en la query que trae los results de filtros, o sea con un atom interno nomas
(defn send-pinned-dim-query [{:keys [designer] :as db} {:keys [name] :as dim} & [{:as search-filter}]]
  (let [q (cond-> {:cube (get-in designer [:report :cube])
                   :filters (remove-dim-unless-time dim (:filters designer))
                   :splits [dim]
                   :measures (vector (get-in designer [:pinboard :measure]))}
                  search-filter (update :filters add-dimension search-filter))]
    (send-query db (unexpand q) [:designer :pinboard-results name])))

(defn send-pinboard-queries
  ([db] (send-pinboard-queries db nil))
  ([db except-dim]
   (->> (get-in db [:designer :pinboard :dimensions])
        (remove-dim-unless-time except-dim)
        (reduce #(send-pinned-dim-query %1 %2) db))))

(def debounce-dispatch (debounce dispatch 500))

(defn build-visualization [results {:keys [cube] :as report}]
  (let [cube (get-cube cube)]
    (assert cube)
    (-> (report->designer report cube)
        (select-keys [:viztype :splits :measures])
        (assoc :results results))))

(defn- default-measures [{:keys [measures]}]
  (->> (if (some :favorite measures)
         (filter :favorite measures)
         (take 3 measures))
       (mapv :name)))

(defn build-new-report [{:keys [name] :as cube}]
  (let [measures (default-measures cube)]
    {:cube name
     :name (t :reports/new)
     :viztype "totals"
     :measures measures
     :filters [{:name "__time" :period "latest-day"}]}))

;;;;; Components

(defn panel-header [text & actions]
  [:h2.ui.sub.header text
   (when (seq actions) (into [:div.actions] actions))])

(defn description-help-icon [{:keys [description]}]
  (when (seq description)
    [:i.question.circle.outline.icon {:ref (tooltip description {:position "right center" :delay 250})}]))

(defn search-button [searching]
  [:i.search.link.icon {:on-click #(swap! searching not)}])

(defn highlight [value search]
  (if (seq search)
    (let [[_ pre bold post] (re-find (re-pattern (str "(?i)(.*?)(" (regex-escape search) ")(.*)")) value)]
      [:div.segment-value pre [:span.bold bold] post])
    [:div.segment-value value]))
