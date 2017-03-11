(ns pivot.settings
  (:require [pivot.i18n :refer [t]]
            [pivot.components :refer [page-title]]))

(defn page []
 [:div.ui.container.railed
  [page-title (t :settings/title) (t :settings/subtitle) "settings"]
  [:h2.ui.dividing.header (t :settings/language)]
  [:div "Aca va el input"]
  [:h2.ui.dividing.header (t :settings/users)]
  [:div "TODO Tabla de users"]])
