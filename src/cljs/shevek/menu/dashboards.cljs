(ns shevek.menu.dashboards
  (:require [shevek.reflow.db :as db]
            [shevek.i18n :refer [t]]
            [shevek.lib.react :refer [rmap]]
            [shevek.pages.home.dashboards :refer [fetch-dashboards]]
            [shevek.navigation :refer [current-page?]]
            [shevek.components.popup :refer [show-popup close-popup]]))

(defn- dashboard-item [{:keys [id name description]}]
  [:a.item {:href (str "/dashboards/" id) :on-click close-popup}
   [:div.header name]
   [:div.description description]])

(defn- popup-content []
  (when-not (current-page? :home) ; No need to fetch the dashboards again when we are on the home page
    (fetch-dashboards))
  (fn []
    (let [dashboards (filter :id (db/get :dashboards))] ; Ignore dashboards that are being created
      [:div#dashboards-popup
       [:a.ui.compact.button {:href "/dashboards/new" :on-click close-popup} (t :actions/new)]
       (if (seq dashboards)
         [:div.ui.relaxed.middle.aligned.selection.list
          (rmap dashboard-item :id dashboards)]
         [:div (t :dashboards/missing)])])))

(defn dashboards-menu []
  [:a.item {:on-click #(show-popup % popup-content {:position "bottom left"})
            :class (when (current-page? :dashboard) "active")}
   [:i.block.layout.icon] (or (and (current-page? :dashboard)
                                   (db/get-in [:current-dashboard :name]))
                              (t :dashboards/title))])
