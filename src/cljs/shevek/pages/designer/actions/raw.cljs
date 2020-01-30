(ns shevek.pages.designer.actions.raw
  (:require [shevek.reflow.core :refer [dispatch] :refer-macros [defevh]]
            [shevek.schemas.conversion :refer [designer->raw-query]]
            [shevek.rpc :as rpc]
            [shevek.i18n :refer [t]]
            [shevek.components.popup :refer [tooltip]]
            [shevek.components.modal :refer [show-modal]]
            [shevek.pages.designer.helpers :refer [current-cube]]
            [shevek.domain.dw :refer [dimension-value measure-value]]
            [shevek.pages.designer.filters :refer [slice->filters filter-title]]
            [shevek.domain.dimension :refer [time-dimension? merge-dimensions]]
            [shevek.lib.time.ext :refer [format-time]]
            [cuerdas.core :as str]
            [reagent.core :as r]))

(def limit 100)
(def raw-data (r/atom {}))

(defn- format-dimension [dim result]
  (let [value (dimension-value dim result)]
    (cond
      (sequential? value) (str/join ", " value)
      (time-dimension? dim) (format-time value)
      :else value)))

(defn raw-data-table []
  (let [results (get-in @raw-data [:response :results])
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
          (doall
           (for [{:keys [name] :as m} measures]
             [:td.measure {:key name} (measure-value m result)]))]))]]))

(defn filters->str [filters]
  (into [:span.filters]
        (->> (map filter-title filters)
             (interpose ", "))))

(defn- raw-data-modal []
  (fn []
    [:div.ui.modal.large.raw-data
     [:div.header (t :raw-data/title)]
     [:div.content
      [:p (t :raw-data/showing limit) [filters->str (:filter @raw-data)]]
      (if (:loading? @raw-data)
        [:div.ui.basic.segment.loading]
        [:div.table-container [raw-data-table]])]
     [:div.actions
      [:button.ui.cancel.button (t :actions/close)]]]))

(defevh :designer/raw-data-requested [{:keys [designer] :as db} slice]
  (reset! raw-data {:loading? true})
  (show-modal [raw-data-modal])
  (let [designer (cond-> designer
                   slice (update :filters merge-dimensions (slice->filters slice "include")))
        q (-> (designer->raw-query designer)
              (assoc-in [:paging :threshold] limit))
        handler #(reset! raw-data {:loading? false :response % :filter (:filters designer)})]
    (rpc/call "querying/raw-query" :args [q] :handler handler)))

(defn raw-data-button []
  [:button.ui.default.icon.button
   {:ref (tooltip (t :raw-data/title))
    :on-click #(dispatch :designer/raw-data-requested)
    :data-tid "raw-data"}
   [:i.database.icon]])
