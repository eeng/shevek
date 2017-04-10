(ns shevek.layout
  (:require-macros [reflow.macros :refer [defevh]])
  (:require [shevek.i18n :refer [t]]
            [secretary.core :as secretary :refer-macros [defroute]]
            [reflow.core :refer [dispatch]]
            [reflow.db :as db]
            [shevek.rpc :refer [loading?]]
            [shevek.components :refer [make-dropdown modal]]
            [shevek.dashboard :as dashboard]
            [shevek.settings.page :as settings]
            [shevek.viewer.page :as viewer]
            [shevek.viewer.shared :refer [current-cube-name]]
            [shevek.dw :as dw]))

(def pages
  {:dashboard #'dashboard/page
   :settings #'settings/page
   :viewer #'viewer/page})

(defevh :navigate [db page]
  (assoc db :page page))

(defroute "/" []
  (dispatch :navigate :dashboard))

(defroute "/settings" []
  (dispatch :navigate :settings))

(defroute "/cubes/:cube" [cube]
  (dispatch :cube-selected cube))

; TODO faltan las rutas para errores

(defn current-page? [page]
  (= (db/get :page) page))

(defn current-page-class [page]
  (when (current-page? page) "active"))

(defn cubes-menu []
  (dw/fetch-cubes)
  (fn []
    (let [cube-name (current-cube-name)
          cube-title (db/get-in [:cubes cube-name :title])
          cubes (dw/cubes-list)]
      (when (seq cubes)
        [make-dropdown {}
         [:div.ui.dropdown.item
          [:i.cubes.icon]
          [:div.text (if (current-page? :viewer) cube-title (t :cubes/menu))]
          [:i.dropdown.icon]
          [:div.menu
           (for [{:keys [name title]} cubes]
             ^{:key name}
             [:a.item {:href (str "#/cubes/" name)
                       :class (when (= cube-name name) "active")}
              title])]]]))))

(defn- menu []
  [:div.ui.fixed.inverted.menu
   [:a.item {:href "#/" :class (current-page-class :dashboard)}
    [:i.block.layout.icon] (t :dashboard/title)]
   [cubes-menu]
   [:div.right.menu
    (when (loading?) [:div.item [:i.repeat.loading.icon]])
    [:a.item {:href "#/settings" :class (current-page-class :settings)}
     [:i.settings.icon] (t :settings/title)]
    [:a.item {:href "#/logout"} [:i.sign.out.icon] (t :menu/logout)]]])

(defn layout []
  [:div
   [menu]
   [:div.page
    [(get pages (db/get :page) :div)]]
   [modal]])
