(ns shevek.pages.profile.page
  (:require [shevek.components.layout :refer [page-with-header]]
            [shevek.domain.auth :refer [current-user]]
            [shevek.i18n :refer [t]]
            [shevek.navigation :refer [active-class-when-page]]
            [shevek.pages.profile.preferences :refer [preferences-panel]]
            [shevek.pages.profile.password :refer [password-panel]]
            [shevek.pages.profile.helpers :refer [avatar]]))

(defn page []
  (let [{:keys [fullname username] :as user} (current-user)]
    [page-with-header {:title fullname :subtitle username :image [avatar user] :id "profile"}
     [:div.ui.secondary.menu
      [:a.item {:href "/profile" :class (active-class-when-page :profile/preferences)}
       [:i.sliders.icon]
       (t :profile/preferences)]
      [:a.item {:href "/profile/password" :class (active-class-when-page :profile/password)}
       [:i.lock.icon]
       (t :profile/password)]]
     [:div.ui.tab.segment {:class (active-class-when-page :profile/preferences)}
      [preferences-panel]]
     [:div.ui.tab.segment {:class (active-class-when-page :profile/password)}
      [password-panel]]]))
