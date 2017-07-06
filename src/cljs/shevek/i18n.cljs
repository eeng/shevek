(ns shevek.i18n
  (:require [tongue.core :as tongue]
            [reflow.db :as db]))

(def translations
  {:en
   {:menu {:logout "Logout"}
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
            :sort-by "Sort By"
            :granularity "Granularity"}
    :cubes.period {:relative "Relative"
                   :specific "Specific"
                   :latest "Latest"
                   :latest-hour "Latest Hour"
                   :latest-day "Latest Day"
                   :latest-7days "Latest 7 Days"
                   :latest-30days "Latest 30 Days"
                   :latest-90days "Latest 90 Days"
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
                   :type "Type"
                   :format "Format"
                   :expression "Expression"}
    :reports {:menu "Reports"
              :title "Saved Reports"
              :pinned "Favorite Reports"
              :name "Name"
              :description "Description"
              :pin-in-dashboard "Pin in dashboard"
              :saved "Report '{1}' saved!"
              :none "Save your favorite reports with the \"Pin in dashboard\" option so they appear here."}
    :settings {:menu "Settings"
               :lang "Language"
               :update-now "Update Now"
               :auto-refresh "Auto Update"
               :auto-refresh-opts (fn [] [["Off" 0] ["Every 10 seconds" 10] ["Every 30 seconds" 30] ["Every minute" 60] ["Every 5 minutes" 300] ["Every 15 minutes" 900] ["Every 30 minutes" 1800]])}
    :admin {:menu "Admin"
            :title "Administration"
            :subtitle "Configure the users, cube descriptions and other stuff in this page"
            :users "Users"}
    :users {:username "Username"
            :fullname "Full Name"
            :email "Email"
            :password "Password"
            :password-confirmation "Password Confirmation"
            :invalid-credentials "Invalid username or password"}
    :date-formats {:minute "MMM d, h:mma"
                   :hour "MMM d, yyyy ha"
                   :day "MMM d, yyyy"
                   :month "MMM yyyy"}
    :actions {:ok "Accept"
              :cancel "Cancel"
              :edit "Modify"
              :save "Save"
              :save-as "Save As"
              :new "New"
              :delete "Delete"
              :close "Close"}
    :input {:search "Search"}
    :validation {:required "can't be blank"
                 :regex "doesn't match pattern"
                 :email "is not a valid email address"
                 :password "must have a combination of at least 7 letters and numbers (or symbols)"
                 :confirmation "doesn't match the previous value"}}

   :es
   {:menu {:logout "Salir"}
    :dashboard {:subtitle "Muestra las fuentes de datos disponibles y sus reportes favoritos"}
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
            :sort-by "Ordenar Por"
            :granularity "Granularidad"}
    :cubes.period {:relative "Relativo"
                   :specific "Específico"
                   :latest "Ultimo"
                   :latest-hour "Ultima Hora"
                   :latest-day "Ultimo Día"
                   :latest-7days "Ultimos 7 Días"
                   :latest-30days "Ultimos 30 Días"
                   :latest-90days "Ultimos 90 Días"
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
                   :type "Tipo"
                   :format "Formato"
                   :expression "Expresión"}
    :reports {:menu "Reportes"
              :title "Reportes"
              :pinned "Reportes Favoritos"
              :name "Nombre"
              :description "Descripción"
              :pin-in-dashboard "Mostrar en dashboard"
              :saved "Reporte '{1}' guardado correctamente"
              :none "Guardá tus reportes favoritos con la opción 'Mostrar en dashboard' para que aparezcan aquí."}
    :settings {:menu "Preferencias"
               :lang "Lenguaje"
               :update-now "Actualizar Ahora"
               :auto-refresh "Refrescar Automáticamente"
               :auto-refresh-opts (fn [] [["Nunca" 0] ["Cada 10 segundos" 10] ["Cada 30 segundos" 30] ["Cada 1 minuto" 60] ["Cada 5 minutos" 300] ["Cada 15 minutos" 900] ["Cada 30 minutos" 1800]])}
    :admin {:title "Administración"
            :subtitle "Configure los usuarios, descripciones de cubos y otras cuestiones aquí"
            :users "Usuarios"}
    :users {:username "Usuario"
            :fullname "Nombre"
            :email "Email"
            :password-confirmation "Confirmación de Password"
            :invalid-credentials "Usuario y/o password incorrecto"}
    :date-formats {:minute "dd/MM HH:mm"
                   :hour "dd/MM/yy H 'hs'"
                   :day "dd/MM/yy"
                   :month "MM/yy"}
    :actions {:ok "Aceptar"
              :cancel "Cancelar"
              :edit "Editar"
              :save "Guardar"
              :save-as "Guardar Como"
              :new "Nuevo"
              :delete "Eliminar"
              :close "Cerrar"}
    :input {:search "Buscar"}
    :validation {:required "este campo es obligatorio"
                 :email "no es una dirección válida"
                 :password "debería tener al menos 7 letras y números (o símbolos)"
                 :confirmation "no coincide con el valor anterior"}}

   :tongue/fallback :en})

(def translate (tongue/build-translate translations))

(defn lang []
  (keyword (db/get-in [:settings :lang] "en")))

(defn t [& args]
  (apply translate (lang) args))

(defn translation [& args]
  (get-in translations (into [(lang)] args)))
