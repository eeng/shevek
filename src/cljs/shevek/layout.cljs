(ns shevek.layout
  (:require [shevek.i18n :refer [t]]
            [shevek.reflow.db :as db]
            [shevek.reflow.core :refer [dispatch]]
            [shevek.navegation :refer [current-page? current-page]]
            [shevek.rpc :refer [loading?]]
            [shevek.components.popup :refer [popup]]
            [shevek.components.modal :refer [modal]]
            [shevek.login :as login :refer [logged-in?]]
            [shevek.dashboard :as dashboard]
            [shevek.admin.page :as admin]
            [shevek.settings :refer [settings-menu]]
            [shevek.notification :refer [notification]]
            [shevek.viewer.page :as viewer]
            [shevek.reports.menu :refer [reports-menu]]
            [shevek.menu.cubes :refer [cubes-menu]]
            [shevek.menu.share :refer [share-menu]]
            [shevek.login :refer [current-user]]))

(def pages
  {:login #'login/page
   :dashboard #'dashboard/page
   :admin #'admin/page
   :viewer #'viewer/page})

(defn current-page-class [page]
  (when (current-page? page) "active"))

(defn- menu []
  [:div.ui.fixed.inverted.menu
   [:a.item {:href "#/" :class (current-page-class :dashboard)}
    [:i.block.layout.icon] (t :dashboard/title)]
   [cubes-menu]
   [reports-menu]
   [:div.right.menu
    [share-menu]
    [:div.item
     [:i.user.icon]
     (:fullname (current-user))]
    [settings-menu]
    [:a.item {:href "#/admin" :class (current-page-class :admin)}
     [:i.users.icon] (t :admin/menu)]
    [:a.item {:on-click #(dispatch :logout)} [:i.sign.out.icon] (t :menu/logout)]]])

(defn layout []
  (let [page (if (logged-in?) (current-page) :login)]
    [:div
     (when (logged-in?) [menu])
     [:div.page
      [(get pages page :div)]]
     [popup]
     [modal]
     [notification]]))
