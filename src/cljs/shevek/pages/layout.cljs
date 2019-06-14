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
            [shevek.pages.cubes.page :as cubes]
            [shevek.pages.dashboards.page :as dashboards]
            [shevek.pages.dashboards.show :as dashboard]
            [shevek.pages.reports.page :as reports]
            [shevek.pages.designer.page :as designer]
            [shevek.pages.profile.page :as profile]
            [shevek.pages.configuration.page :as configuration]
            [shevek.components.notification :refer [notification]]
            [shevek.components.virtualized-examples :refer [example-page]]))

(defn page-component-for [page]
  (case page
    :login #'login/page
    ; :home #'home/page
    :home #'example-page
    :designer #'designer/page
    :dashboard #'dashboard/page
    :dashboards #'dashboards/page
    :cubes #'cubes/page
    :reports #'reports/page
    :configuration #'configuration/page
    :profile/preferences #'profile/page
    :profile/password #'profile/page
    :error #'error/page))

(defn layout []
  (when (db/get :initialized)
    (let [page (if (logged-in?) (current-page) :login)
          page-component (page-component-for page)]
      [:div.layout
       (when (logged-in?) [sidebar])
       [:div.page-container
        [page-component]]
       [popup]
       [modal]
       [notification]])))
