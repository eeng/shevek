(ns shevek.pages.sidebar
  (:require [reagent.core :as r]
            [shevek.navigation :refer [active-class-when-page]]
            [shevek.reflow.core :refer [dispatch] :refer-macros [defevh]]
            [shevek.i18n :refer [t]]
            [shevek.reflow.db :as db]
            [shevek.domain.auth :refer [admin? current-user]]
            [shevek.components.popup :as p]
            [shevek.pages.profile.helpers :refer [avatar]]))

(defn tooltip [i18n-key]
  (p/tooltip (t i18n-key) {:position "right center" :delay 700}))

(defn sidebar* []
  (if (db/get-in [:preferences :sidebar-visible])
    [:div#sidebar.ui.inverted.left.fixed.vertical.icon.menu
     [:a.item {:on-click #(dispatch :preferences/save {:sidebar-visible false})}
      [:i.bars.icon]]
     [:div.item.top.divider]
     [:a.item {:href "/"
               :class (active-class-when-page :home)
               :ref (tooltip :home/menu)}
      [:i.home.icon]]
     [:a.item {:href "/cubes"
               :class (active-class-when-page :cubes)
               :ref (tooltip :cubes/title)}
      [:i.cubes.icon]]
     [:a.item {:href "/dashboards"
               :class (active-class-when-page :dashboards)
               :ref (tooltip :dashboards/title)
               :data-tid "sidebar-dashboards"}
      [:i.block.layout.icon]]
     [:a.item {:href "/reports"
               :class (active-class-when-page :reports)
               :ref (tooltip :reports/title)
               :data-tid "sidebar-reports"}
      [:i.line.chart.icon]]
     (when (admin?)
       [:a.icon.item {:href "/configuration"
                      :class (active-class-when-page :configuration)
                      :ref (tooltip :configuration/menu)
                      :data-tid "sidebar-config"}
        [:i.setting.icon]])
     [:div.item.middle.divider]
     [:a.item.profile {:href "/profile"
                       :class (active-class-when-page :profile/preferences :profile/password)
                       :ref (tooltip :profile/menu)
                       :data-tid "sidebar-profile"}
      [avatar (current-user)]]
     [:a.item {:on-click #(dispatch :logout)
               :ref (tooltip :sessions/logout)
               :data-tid "sidebar-logout"}
      [:i.sign.out.icon]]
     [:div.item.bottom.divider]]

    [:div#sidebar.ui.inverted.left.fixed.compact.vertical.icon.menu
     [:a.item
      {:on-click #(dispatch :preferences/save {:sidebar-visible true})}
      [:i.bars.icon]]]))

(defn sidebar []
  (r/create-class {:reagent-render sidebar*
                   ; So the dashboard grid resizes accordingly
                   :component-did-update #(.dispatchEvent js/window (js/Event. "resize"))}))
