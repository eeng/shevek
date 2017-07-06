(ns shevek.layout
  (:require [shevek.i18n :refer [t]]
            [reflow.db :as db]
            [reflow.core :refer [dispatch]]
            [shevek.navegation :refer [current-page? current-page]]
            [shevek.rpc :refer [loading?]]
            [shevek.components :refer [controlled-popup]]
            [shevek.components.modal :refer [modal]]
            [shevek.login :as login :refer [logged-in?]]
            [shevek.dashboard :as dashboard]
            [shevek.admin.page :as admin]
            [shevek.settings :refer [settings-menu]]
            [shevek.notification :refer [notification]]
            [shevek.viewer.page :as viewer]
            [shevek.viewer.shared :refer [current-cube-name]]
            [shevek.reports.menu :refer [reports-menu]]
            [shevek.dw :as dw]))

(def pages
  {:login #'login/page
   :dashboard #'dashboard/page
   :admin #'admin/page
   :viewer #'viewer/page})

(defn current-page-class [page]
  (when (current-page? page) "active"))

(defn- cubes-popup-content [{:keys [close]}]
  (let [cubes (dw/cubes-list)
        select-cube #(do (dispatch :cube-selected %) (close))]
    [:div#cubes-popup
     [:h3.ui.sub.orange.header (t :cubes/title)]
     (if (seq cubes)
       [:div.ui.relaxed.middle.aligned.selection.list
        (doall
          (for [{:keys [name title description] :or {description (t :cubes/no-desc)}} cubes
                :let [selected? (and (current-page? :viewer) (= name (current-cube-name)))]]
            [:div.item {:key name :on-click #(select-cube name)}
             [:i.large.middle.aligned.cube.icon {:class (when selected? "orange")}]
             [:div.content
              [:div.header title]
              [:div.description description]]]))]
       [:div (t :cubes/no-results)])]))

(defn- cubes-popup-activator [popup]
  [:a#cubes-menu.item {:on-click (popup :toggle)}
   [:i.cubes.icon]
   (if (current-page? :viewer)
     (db/get-in [:cubes (current-cube-name) :title])
     (t :cubes/menu))])

(defn- cubes-menu []
  (dw/fetch-cubes)
  (fn []
    [(controlled-popup cubes-popup-activator cubes-popup-content {:position "bottom left"})]))

(defn- menu []
  [:div.ui.fixed.inverted.menu
   [:a.item {:href "#/" :class (current-page-class :dashboard)}
    [:i.block.layout.icon] (t :dashboard/title)]
   [cubes-menu]
   [reports-menu]
   [:div.right.menu
    [settings-menu]
    [:a.item {:href "#/admin" :class (current-page-class :admin)}
     [:i.users.icon] (t :admin/menu)]
    [:a.item {:href "#/logout"} [:i.sign.out.icon] (t :menu/logout)]]])

(defn layout []
  (let [page (if (logged-in?) (current-page) :login)]
    [:div
     (when (logged-in?) [menu])
     [:div.page
      [(get pages page :div)]]
     [modal]
     [notification]]))
