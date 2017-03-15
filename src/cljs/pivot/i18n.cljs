(ns pivot.i18n
  (:require [tongue.core :as tongue]
            [reflow.db :as db]))

(def translations
  {:en
   {:menu {:logout "Logout"
           :settings "Settings"}
    :dashboard {:title "Dashboard"
                :subtitle "Pin here your favorite reports for easy access"}
    :cubes {:title "Data Cubes"
            :menu "Cubes"
            :missing "There aren't any data cubes defined."
            :dimensions "Dimensions"
            :measures "Measures"
            :filter "Filter"
            :split "Split"
            :pin "Pin"
            :pinboard "Pinboard"
            :no-desc "No description."
            :no-pinned "Drag or click dimensions to pin them"
            :no-measures "Please select at least one measure"}
    :settings {:title "Settings"
               :subtitle "Configure the application language, users and other stuff in this page"
               :language "Language"
               :users "Users"}}

   :es
   {:menu {:logout "Salir"
           :settings "Configuración"}
    :dashboard {:title "Centro de Control"
                :subtitle "Muestra las fuentes de datos disponibles y sus reportes favoritos"}
    :cubes {:title "Cubos de Datos"
            :menu "Cubos"
            :missing "No hay cubos definidos."
            :dimensions "Dimensiones"
            :measures "Métricas"
            :filter "Filtros"
            :no-desc "Sin descripción."
            :no-pinned "Arrastre o clickee dimensiones para fijarlas aquí"
            :no-measures "Por favor seleccione al menos una métrica"}
    :settings {:title "Configuración"
               :subtitle "Configure el lenguaje de la aplicación, los usuarios y otras cuestiones aquí"
               :language "Lenguaje"
               :users "Usuarios"}}

   :tongue/fallback :en})

(def translate (tongue/build-translate translations))

(defn t [& args]
  (apply translate (db/get :lang "en") args))
