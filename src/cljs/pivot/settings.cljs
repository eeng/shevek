(ns pivot.settings
  (:require [pivot.i18n :refer [t]]))

(defn page []
 [:div.ui.container.railed
  [:h1.ui.header
   [:i.settings.icon]
   [:div.content (t :settings/title)
    [:div.sub.header (t :settings/subtitle)]]]
  [:h2.ui.dividing.header (t :settings/language)]
  [:div "Aca va el input"]
  [:h2.ui.dividing.header (t :settings/users)]
  [:div "TODO Tabla de users"]])
