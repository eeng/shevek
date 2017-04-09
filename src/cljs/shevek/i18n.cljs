(ns shevek.i18n
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
            :no-desc "No description"
            :no-pinned "Drag or click dimensions to pin them"
            :no-measures "Please select at least one measure"
            :no-results "No results where found"
            :limit "Limit"
            :sort-by "Sort By"}
    :cubes.period {:relative "Relative"
                   :specific "Specific"
                   :latest "Latest"
                   :latest-hour "Latest Hour"
                   :latest-6hours "Latest 6 Hours"
                   :latest-day "Latest Day"
                   :latest-7days "Latest 7 Days"
                   :latest-30days "Latest 30 Days"
                   :current "Current"
                   :current-day "Current Day"
                   :current-week "Current Week"
                   :current-month "Current Month"
                   :current-quarter "Current Quarter"
                   :current-year "Current Year"
                   :previous "Previous"
                   :previous-day "Previous Day"
                   :previous-week "Previous Week"
                   :previous-month "Previous Month"
                   :previous-quarter "Previous Quarter"
                   :previous-year "Previous Year"}
    :cubes.operator {:include "Include"
                     :exclude "Exclude"}
    :cubes.schema {:name "Name"
                   :title "Title"
                   :description "Description"
                   :type "Type"}
    :settings {:title "Settings"
               :subtitle "Configure the application language, users and other stuff in this page"
               :language "Language"
               :users "Users"}
    :users {:username "User Name"
            :fullname "Full Name"
            :email "Email"
            :password "Password"
            :password-confirmation "Password Confirmation"}
    :date-formats {:minute "MMM d, h:mma"
                   :hour "MMM d yyyy, ha"
                   :day "MMM d yyyy"
                   :month "MMM yyyy"}
    :actions {:ok "Accept"
              :cancel "Cancel"
              :edit "Modify"
              :save "Save"
              :new "New"
              :delete "Delete"
              :close "Close"}
    :input {:search "Search"}
    :validation {:required "can't be blank"
                 :regex "doesn't match pattern"
                 :email "is not a valid email address"
                 :password "must have a combination of at least 7 letters and numbers (or symbols)"}}

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
            :no-desc "Sin descripción"
            :no-pinned "Arrastre o clickee dimensiones para fijarlas aquí"
            :no-measures "Por favor seleccione al menos una métrica"
            :no-results "No se encontraron resultados"
            :limit "Límite"
            :sort-by "Ordenar Por"}
    :cubes.period {:relative "Relativo"
                   :specific "Específico"
                   :latest "Ultimo"
                   :latest-hour "Ultima Hora"
                   :latest-6hours "Ultimas 6 Horas"
                   :latest-day "Ultimo Día"
                   :latest-7days "Ultimos 7 Días"
                   :latest-30days "Ultimos 30 Días"
                   :current "Actual"
                   :current-day "Día de Hoy"
                   :current-week "Esta Semana"
                   :current-month "Este Mes"
                   :current-quarter "Este Trimestre"
                   :current-year "Este Año"
                   :previous "Previo"
                   :previous-day "Día de Ayer"
                   :previous-week "Semana Pasada"
                   :previous-month "Mes Pasado"
                   :previous-quarter "Trimestre Pasado"
                   :previous-year "Año Pasado"}
    :cubes.operator {:include "Incluir"
                     :exclude "Excluir"}
    :cubes.schema {:name "Nombre"
                   :title "Título"
                   :description "Descripción"
                   :type "Tipo"}
    :settings {:title "Configuración"
               :subtitle "Configure el lenguaje de la aplicación, los usuarios y otras cuestiones aquí"
               :language "Lenguaje"
               :users "Usuarios"}
    :users {:username "Usuario"
            :fullname "Nombre"
            :email "Email"
            :password-confirmation "Confirmación de Password"}
    :date-formats {:minute "dd/MM HH:mm"
                   :hour "dd/MM/yyyy H 'hs'"
                   :day "dd/MM/yyyy"
                   :month "MM/yyyy"}
    :actions {:ok "Aceptar"
              :cancel "Cancelar"
              :edit "Editar"
              :save "Guardar"
              :new "Nuevo"
              :delete "Eliminar"
              :close "Cerrar"}
    :input {:search "Buscar"}
    :validation {:required "este campo es obligatorio"
                 :email "no es una dirección válida"
                 :password "debería tener al menos 7 letras y números (o símbolos)"}}

   :tongue/fallback :en})

(def translate (tongue/build-translate translations))

(defn lang []
  (keyword (db/get-in [:settings :lang] "en")))

(defn t [& args]
  (apply translate (lang) args))

(defn translation [& args]
  (get-in translations (into [(lang)] args)))
