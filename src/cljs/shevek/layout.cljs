(ns shevek.layout
  (:require [shevek.i18n :refer [t]]
            [shevek.reflow.db :as db]
            [shevek.reflow.core :refer [dispatch] :refer-macros [defevh]]
            [shevek.navigation :refer [current-page? current-page]]
            [shevek.components.popup :refer [popup tooltip]]
            [shevek.components.modal :refer [modal]]
            [shevek.domain.auth :refer [logged-in? admin?]]
            [shevek.pages.login :as login]
            [shevek.pages.home.page :as home]
            [shevek.pages.error :as error]
            [shevek.pages.dashboard :as dashboard]
            [shevek.pages.admin.page :as admin]
            [shevek.components.notification :refer [notification]]
            [shevek.viewer.page :as viewer]
            [shevek.menu.cubes :refer [cubes-menu]]
            [shevek.menu.reports :refer [reports-menu]]
            [shevek.menu.dashboards :refer [dashboards-menu]]
            [shevek.menu.share :refer [share-menu]]
            [shevek.menu.fullscreen :refer [fullscreen-menu]]
            [shevek.menu.settings :refer [settings-menu]]
            [shevek.pages.account :as account :refer [account-menu]]))

(def pages
  {:login #'login/page
   :home #'home/page
   :dashboard #'dashboard/page
   :admin #'admin/page
   :viewer #'viewer/page
   :account #'account/page
   :error #'error/page})

(defn current-page-class [page]
  (when (current-page? page) "active"))

(defn- menu []
  [:div.ui.fixed.inverted.menu
   [:a.item {:href "/" :class (current-page-class :home)}
    [:i.home.layout.icon] (t :home/menu)]
   [cubes-menu]
   [dashboards-menu]
   [reports-menu]
   [:div.right.menu
    (when (current-page? :viewer) [fullscreen-menu])
    (when (current-page? :viewer) [share-menu])
    [settings-menu]
    (when (admin?)
      [:a.icon.item {:href "/admin" :class (current-page-class :admin) :ref (tooltip (t :admin/menu))}
       [:i.users.icon]])
    [account-menu (current-page-class :account)]
    [:a.item {:on-click #(dispatch :logout)}
     [:i.sign.out.icon] (t :menu/logout)]]])

(defn layout []
  (when-let [page (cond
                    (logged-in?) (current-page)
                    (db/get :user-restored) :login)]
    [:div
     (when (logged-in?) [menu])
     [:div.page
      [(get pages page :div)]]
     [popup]
     [modal]
     [notification]]))
