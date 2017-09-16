(ns shevek.menu.cubes
  (:require [shevek.i18n :refer [t]]
            [shevek.reflow.db :as db]
            [shevek.reflow.core :refer [dispatch] :refer-macros [defevh]]
            [shevek.navigation :refer [current-page?]]
            [shevek.components.popup :refer [show-popup close-popup]]
            [shevek.viewer.shared :refer [current-cube-name]]
            [shevek.lib.dw.cubes :refer [fetch-cubes cubes-list]]))

(defn- popup-content []
  (let [cubes (cubes-list)
        select-cube #(do (dispatch :cube-selected %) (close-popup))]
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
       [:div (t :errors/no-results)])]))

(defn- cubes-menu []
  (when-not (current-page? :home) ; No need to fetch the cubes again when we are on the home page
    (fetch-cubes))
  (fn []
    [:a#cubes-menu.item {:on-click #(show-popup % popup-content {:position "bottom left"})}
     [:i.cubes.icon]
     (if (current-page? :viewer)
       (db/get-in [:cubes (current-cube-name) :title])
       (t :cubes/menu))]))
