(ns shevek.layout
  (:require [shevek.i18n :refer [t]]
            [shevek.reflow.db :as db]
            [shevek.reflow.core :refer [dispatch] :refer-macros [defevh]]
            [shevek.navigation :refer [current-page? current-page]]
            [shevek.rpc :refer [loading?]]
            [shevek.components.popup :refer [popup]]
            [shevek.components.modal :refer [modal]]
            [shevek.login :as login :refer [logged-in? admin?]]
            [shevek.home.page :as home]
            [shevek.dashboard :as dashboard]
            [shevek.admin.page :as admin]
            [shevek.notification :refer [notification]]
            [shevek.viewer.page :as viewer]
            [shevek.menu.cubes :refer [cubes-menu]]
            [shevek.menu.reports :refer [reports-menu]]
            [shevek.menu.share :refer [share-menu]]
            [shevek.menu.settings :refer [settings-menu]]
            [shevek.menu.account :as account :refer [account-menu]]))

(def pages
  {:login #'login/page
   :home #'home/page
   :dashboard #'dashboard/page
   :admin #'admin/page
   :viewer #'viewer/page
   :account #'account/page})

(defn current-page-class [page]
  (when (current-page? page) "active"))

(defn- menu []
  [:div.ui.fixed.inverted.menu
   [:a.item {:href "#/" :class (current-page-class :home)}
    [:i.home.layout.icon] (t :home/menu)]
   [cubes-menu]
   [reports-menu]
   [:div.right.menu
    (when (current-page? :viewer) [share-menu])
    [settings-menu]
    (when (admin?)
      [:a.icon.item {:href "#/admin" :class (current-page-class :admin) :title (t :admin/menu)}
       [:i.users.icon]])
    [account-menu (current-page-class :account)]
    [:a.item {:on-click #(dispatch :logout)}
     [:i.sign.out.icon] (t :menu/logout)]]])

(defn layout []
  (let [page (if (logged-in?) (current-page) :login)]
    [:div
     (when (logged-in?) [menu])
     [:div.page
      [(get pages page :div)]]
     [popup]
     [modal]
     [notification]]))
