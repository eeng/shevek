(ns pivot.i18n
  (:require [tongue.core :as tongue]))

(def translations
  {:en
   {:menu {:logout "Logout"
           :settings "Settings"}
    :dashboard {:title "Dashboard"
                :subtitle "Pin here your favorite reports for easy access"}
    :cubes {:title "Data Cubes"
            :subtitle "A cube is a multi-dimensional representation of series of measures"
            :menu "Cubes"
            :missing "There aren't any data cubes defined."}
    :settings {:title "Settings"
               :subtitle "Configure the application language, users and other stuff in this page"
               :language "Language"
               :users "Users"}}
   :es
   {:menu {:logout "Salir"
           :settings "Configuración"}
    :dashboard {:subtitle "Esta sección muestra sus reportes favoritos"}
    :cubes {:title "Cubos de Datos"
            :subtitle "Un cubo es una representación multi-dimensional de una serie de métricas"
            :menu "Cubos"
            :missing "No hay cubos definidos."}
    :settings {:title "Configuración"
               :subtitle "Configure el lenguaje de la aplicación, los usuarios y otras cuestiones aquí"
               :language "Lenguaje"
               :users "Usuarios"}}
   :tongue/fallback :en})

; TODO reemplazar aquí :en con algún dato de la UI y/o del browser
(def t (partial (tongue/build-translate translations) :en))
