(ns shevek.pages.error
  (:require [shevek.reflow.core :refer [dispatch] :refer-macros [defevh]]
            [shevek.reflow.db :as db]
            [shevek.rpc :as rpc]))

(defevh :errors/show-page [db error]
  (dispatch :navigate :error)
  (-> (assoc db :error error)
      (rpc/loaded)))

(defn page []
  (let [{:keys [title message] :or {title "Oops!"}} (db/get :error)]
    [:div#error.ui.grid.centered.container
     [:div.ui.padded.red.segment
      [:div.ui.center.aligned.icon.header
       [:i.frown.outline.icon]
       [:div.title title]
       [:div.sub.header message]]]]))
