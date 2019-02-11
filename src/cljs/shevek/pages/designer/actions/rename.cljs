(ns shevek.pages.designer.actions.rename
  (:require [shevek.reflow.core :refer [dispatch] :refer-macros [defevhi]]
            [shevek.pages.designer.helpers :refer [notify-designer-changes]]
            [shevek.components.editable-text :refer [editable-text]]))

(defevhi :designer/report-renamed [db new-name]
  {:after [notify-designer-changes]}
  (assoc-in db [:designer :report :name] new-name))

(defn report-name [{:keys [name]}]
  [:div.topbar-header
   [editable-text {:text name :on-save #(dispatch :designer/report-renamed %)}]])
