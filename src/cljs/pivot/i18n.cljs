(ns pivot.i18n
  (:require [tongue.core :as tongue]))

(def translations
  {:en
   {:menu {:logout "Logout"}
    :dashboard {:menu "Dashboard"}
    :cubes {:title "Data Cubes"
            :menu "Cubes"
            :missing "There aren't any data cubes defined."}}
   :es
   {:menu {:logout "Salir"}
    :cubes {:title "Cubos de Datos"
            :menu "Cubos"
            :missing "No hay cubos definidos."}}
   :tongue/fallback :en})

; TODO reemplazar aquí :en con algún dato de la UI y/o del browser
(def t (partial (tongue/build-translate translations) :en))
