(ns shevek.viewer.raw
  (:require [shevek.reflow.core :refer [dispatch] :refer-macros [defevh]]
            [shevek.reflow.db :as db]
            [shevek.schemas.conversion :refer [viewer->raw-query]]
            [shevek.rpc :as rpc]
            [shevek.i18n :refer [t]]
            [shevek.components.modal :refer [show-modal]]
            [shevek.viewer.shared :refer [current-cube dimension-value format-measure viewer filter-title]]
            [shevek.viewer.filter :refer [selected-path->filters]]
            [shevek.lib.dw.dims :refer [time-dimension? add-dimension merge-dimensions]]
            [cuerdas.core :as str]
            [shevek.lib.react :refer [rmap]]
            [shevek.lib.string :refer [format-bool]]
            [shevek.lib.dates :refer [format-time]]))

(def limit 100)

(defn- format-dimension [{:keys [type] :as dim} result]
  (let [value (dimension-value dim result)]
    (cond
      (= "BOOL" type) (format-bool value)
      (sequential? value) (str/join ", " value)
      (time-dimension? dim) (format-time value)
      :else value)))

(defn raw-data-table []
  (let [results (db/get-in [:viewer :results :raw :results])
        {:keys [dimensions measures]} (current-cube)]
    [:table.ui.compact.single.line.table
     [:thead>tr
      (for [{:keys [name title]} dimensions]
        [:th {:key name} title])
      (for [{:keys [name title]} measures]
        [:th.measure {:key name} title])]
     [:tbody
      (doall
        (for [result results]
          [:tr {:key (hash result)}
           (doall
             (for [{:keys [name] :as d} dimensions]
               [:td {:key name} (format-dimension d result)]))
           (for [{:keys [name] :as m} measures]
             [:td.measure {:key name} (format-measure m result)])]))]]))

(defn filters->str [filter]
  [:span.filter
   (->> (rmap filter-title :name filter)
        (interpose ", "))])

(defn modal-content []
  [:div.subcontent
   [:p (t :raw-data/showing limit) [filters->str (viewer :raw-data-filter)]]
   (if (rpc/loading? [:viewer :results :raw])
    [:div.ui.basic.segment.loading]
    [:div.table-container [raw-data-table]])])

(defevh :viewer/raw-data-arrived [db results]
  (-> (assoc-in db [:viewer :results :raw] results)
      (rpc/loaded [:viewer :results :raw])))

(defevh :viewer/raw-data-requested [{:keys [viewer] :as db} selected-path]
  (show-modal {:header (t :raw-data/title)
               :content [modal-content]
               :actions [[:div.ui.cancel.button (t :actions/close)]]
               :class "large raw-data"
               :js-opts {:observeChanges false}})
  (let [viewer (cond-> viewer
                       selected-path (update :filter merge-dimensions (selected-path->filters selected-path "include")))
        q (-> (viewer->raw-query viewer)
              (assoc-in [:paging :threshold] limit))]
    (rpc/call "querying/raw-query" :args [q] :handler #(dispatch :viewer/raw-data-arrived %))
    (-> (assoc-in db [:viewer :raw-data-filter] (:filter viewer))
        (rpc/loading [:viewer :results :raw]))))
