(ns shevek.pages.layout
  (:require [shevek.reflow.db :as db]
            [shevek.navigation :refer [current-page]]
            [shevek.components.popup :refer [popup tooltip]]
            [shevek.components.modal :refer [modal]]
            [shevek.domain.auth :refer [logged-in?]]
            [shevek.pages.sidebar :refer [sidebar]]
            [shevek.pages.login :as login]
            [shevek.pages.home.page :as home]
            [shevek.pages.error :as error]
            [shevek.pages.dashboard :as dashboard]
            [shevek.pages.cubes.page :as cubes]
            [shevek.pages.dashboards.page :as dashboards]
            [shevek.pages.reports.page :as reports]
            [shevek.pages.designer.page :as designer]
            [shevek.pages.profile.page :as profile]
            [shevek.pages.configuration.page :as configuration]
            [shevek.components.notification :refer [notification]]))

(def pages
  {:login #'login/page
   :home #'home/page
   :designer #'designer/page
   :dashboard #'dashboard/page
   :dashboards #'dashboards/page
   :cubes #'cubes/page
   :reports #'reports/page
   :configuration #'configuration/page
   :profile/preferences #'profile/page
   :profile/password #'profile/page
   :error #'error/page})

(defn layout []
  (when (db/get :initialized)
    (let [page (if (logged-in?) (current-page) :login)
          page-component (pages page)]
      [:div.layout
       (when (logged-in?) [sidebar])
       [:div.page-container
        (if page-component
          [page-component]
          [:div "Page :" page " not defined"])]
       [popup]
       [modal]
       [notification]])))
