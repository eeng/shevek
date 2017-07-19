(ns shevek.viewer.raw
  (:require-macros [shevek.reflow.macros :refer [defevh]])
  (:require [shevek.reflow.core :refer [dispatch]]
            [shevek.reflow.db :as db]
            [shevek.schemas.conversion :refer [viewer->raw-query]]
            [shevek.rpc :as rpc]
            [shevek.i18n :refer [t]]
            [shevek.components.modal :refer [show-modal]]
            [shevek.viewer.shared :refer [current-cube dimension-value format-measure viewer filter-title]]
            [shevek.lib.dw.dims :refer [time-dimension?]]
            [cuerdas.core :as str]
            [shevek.lib.react :refer [rmap]]
            [shevek.lib.dates :refer [parse-time format-time]]))

(def limit 100)

(defn- format-dimension [{:keys [type] :as dim} result]
  (let [value (dimension-value dim result)]
    (cond
      (= "BOOL" type) (t (keyword (str "boolean/" value)))
      (sequential? value) (str/join ", " value)
      (time-dimension? dim) (-> value parse-time format-time)
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
   [:div (t :raw-data/showing limit) [filters->str (viewer :filter)]]
   (if (rpc/loading? [:results :raw])
    [:div.ui.basic.segment.loading]
    [raw-data-table])])

(defevh :viewer/raw-data-arrived [db results]
  (-> (assoc-in db [:viewer :results :raw] results)
      (rpc/loaded [:results :raw])))

(defevh :viewer/raw-data-requested [{:keys [viewer] :as db}]
  (show-modal {:header (t :raw-data/title)
               :content [modal-content]
               :actions [[:div.ui.cancel.button (t :actions/close)]]
               :class "large raw-data"
               :scrolling true
               :js-opts {:duration 0}}) ; Otherwise if the data arrive before the animation finish it would'n get correctly positioned
  (let [q (-> (viewer->raw-query viewer)
              (assoc-in [:paging :threshold] limit))]
    (rpc/call "querying.api/raw-query" :args [q] :handler #(dispatch :viewer/raw-data-arrived %))
    (rpc/loading db [:results :raw])))
