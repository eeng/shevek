(ns shevek.viewer.raw
  (:require-macros [shevek.reflow.macros :refer [defevh]])
  (:require [shevek.reflow.core :refer [dispatch]]
            [shevek.reflow.db :as db]
            [shevek.schemas.conversion :refer [viewer->raw-query]]
            [shevek.rpc :as rpc]
            [shevek.i18n :refer [t]]
            [shevek.components.modal :refer [show-modal]]
            [shevek.viewer.shared :refer [current-cube format-dimension format-measure]]))

(defevh :viewer/raw-data-arrived [db results]
  (-> (assoc-in db [:viewer :results :raw] results)
      (rpc/loaded [:results :raw])))

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

(defn modal-content []
  [:div#row-data
   [:p "Showing... TODO filter"]
   [raw-data-table]])

(defevh :viewer/raw-data-requested [{:keys [viewer] :as db}]
  (show-modal {:header (t :raw-data/title)
               :content [modal-content]
               :actions [[:div.ui.cancel.button (t :actions/close)]]
               :class "large"})
  (let [q (viewer->raw-query viewer)]
    (rpc/call "querying.api/raw-query" :args [q] :handler #(dispatch :viewer/raw-data-arrived %))
    (rpc/loading db [:results :raw])))

(js/setTimeout #(dispatch :viewer/raw-data-requested) 500)
