(ns shevek.menu.cubes
  (:require [shevek.i18n :refer [t]]
            [shevek.navigation :refer [current-page?]]
            [shevek.components.popup :refer [show-popup close-popup]]
            [shevek.pages.designer.helpers :refer [current-cube]]
            [shevek.domain.cubes :refer [fetch-cubes cubes-list]]))

(defn- popup-content []
  (let [cubes (cubes-list)]
    [:div#cubes-popup
     [:h3.ui.sub.orange.header (t :cubes/title)]
     (if (seq cubes)
       [:div.ui.relaxed.middle.aligned.selection.list
        (doall
          (for [{:keys [name title description] :or {description (t :errors/no-desc)}} cubes
                :let [selected? (and (current-page? :designer) (= name (current-cube :name)))]]
            [:a.item {:key name :href (str "/reports/new/" name) :on-click close-popup}
             [:i.large.middle.aligned.cube.icon {:class (when selected? "orange")}]
             [:div.content
              [:div.header title]
              [:div.description description]]]))]
       [:div (t :errors/no-results)])]))

(defn cubes-menu []
  (when-not (current-page? :home) ; No need to fetch the cubes again when we are on the home page
    (fetch-cubes))
  (fn []
    [:a#cubes-menu.item {:on-click #(show-popup % popup-content {:position "bottom left"})}
     [:i.cubes.icon]
     (if (current-page? :designer)
       (current-cube :title)
       (t :cubes/menu))]))
