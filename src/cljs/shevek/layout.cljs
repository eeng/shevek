(ns shevek.layout
  (:require [shevek.i18n :refer [t]]
            [reflow.db :as db]
            [reflow.core :refer [dispatch]]
            [shevek.navegation :refer [current-page? current-page]]
            [shevek.rpc :refer [loading?]]
            [shevek.components :refer [modal controlled-popup]]
            [shevek.dashboard :as dashboard]
            [shevek.settings.page :as settings]
            [shevek.notification :refer [notification]]
            [shevek.viewer.page :as viewer]
            [shevek.viewer.shared :refer [current-cube-name]]
            [shevek.reports.menu :refer [reports-menu]]
            [shevek.dw :as dw]))

(def pages
  {:dashboard #'dashboard/page
   :settings #'settings/page
   :viewer #'viewer/page})

(defn current-page-class [page]
  (when (current-page? page) "active"))

(defn- cubes-popup-content [{:keys [close]}]
  (dw/fetch-cubes)
  (fn []
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
         [:div (t :cubes/no-results)])])))

(defn- cubes-popup-activator [popup]
  [:a.item {:on-click (popup :toggle)}
   [:i.cubes.icon]
   (if (current-page? :viewer)
     (db/get-in [:cubes (current-cube-name) :title])
     (t :cubes/menu))])

(defn- cubes-menu []
  [(controlled-popup cubes-popup-activator cubes-popup-content {:position "bottom left"})])

(defn- menu []
  [:div.ui.fixed.inverted.menu
   [:a.item {:href "#/" :class (current-page-class :dashboard)}
    [:i.block.layout.icon] (t :dashboard/title)]
   [cubes-menu]
   [reports-menu]
   [:div.right.menu
    [:a.item {:href "#/settings" :class (current-page-class :settings)}
     [:i.settings.icon] (t :settings/title)]
    [:a.item {:href "#/logout"} [:i.sign.out.icon] (t :menu/logout)]]])

(defn layout []
  [:div
   [menu]
   [:div.page
    [(get pages (current-page) :div)]]
   [modal]
   [notification]])
