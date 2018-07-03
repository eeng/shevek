(ns shevek.i18n
  (:require [tongue.core :as tongue]
            [shevek.reflow.db :as db]))

(def translations
  {:en
   {:menu {:logout "Logout"}
    :home {:menu "Home"
           :title "Welcome!"
           :subtitle "What would you like to see today?"}
    :cubes {:title "Data Cubes"
            :menu "Cubes"
            :missing "There aren't any data cubes defined"
            :data-range "Available data range"}
    :dashboards {:title "Dashboards"
                 :missing "There aren't any dashboards created"
                 :saved "Dashboard '{1}' saved!"
                 :deleted "Dashboard '{1}' deleted!"
                 :report-count #(cond
                                  (zero? %) "No reports"
                                  (= 1 %) "1 report"
                                  :else "{1} reports")
                 :no-reports "This dashboard has no reports"
                 :updated-at "Last updated"}
    :reports {:title "Reports"
              :missing "There aren't any reports created"
              :name "Name"
              :description "Description"
              :dashboards "Pin in Dashboards"
              :updated-at "Last updated"
              :saved "Report '{1}' saved!"
              :deleted "Report '{1}' deleted!"
              :unauthorized "Oops! This report is no longer available."}
    :viewer {:dimensions "Dimensions"
             :measures "Measures"
             :filters "Filters"
             :splits "Splits"
             :split-on "Split On"
             :rows "Rows"
             :columns "Columns"
             :pinboard "Pinboard"
             :limit "Limit"
             :sort-by "Sort By"
             :granularity "Granularity"
             :no-pinned "Drag dimensions here to pin them for quick access"
             :no-measures "Please select at least one measure"
             :no-results "No results were found that match the specified search criteria"
             :split-required "At least one split is required for the {1} visualization"
             :too-many-splits-for-chart "A maximum of two splits may be provided for chart visualization"
             :unauthorized "Oops! It seems that the {1} cube is no longer available."
             :toggle-fullscreen "Toggle Fullscreen"}
    :viewer.period {:relative "Relative"
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
                    :previous-year "Previous Year"
                    :from "From"
                    :to "To"}
    :viewer.operator {:include "Include"
                      :exclude "Exclude"}
    :viewer.viztype {:totals "totals"
                     :table "table"
                     :bar-chart "bar chart"
                     :line-chart "line chart"
                     :pie-chart "pie chart"}
    :share {:title "Share"
            :copy-url "Copy URL"
            :copied "URL Copied!"}
    :raw-data {:menu "View raw data"
               :title "Raw Event Data"
               :showing "Showing the first {1} events matching: "
               :button "Raw Data"}
    :settings {:menu "Settings"
               :lang "Language"
               :update-now "Update Now"
               :auto-refresh "Auto Update"
               :auto-refresh-opts (fn [] [["Off" 0] ["Every 10 seconds" 10] ["Every 30 seconds" 30] ["Every minute" 60] ["Every 10 minutes" 600] ["Every 30 minutes" 1800]])
               :abbreviations "Number Abbreviations"
               :abbreviations-opts (fn [] [["Use default settings" "default"] ["Don't use abbreviations" "no"] ["Use abbreviations" "yes"]])}
    :admin {:menu "Manage Users"
            :title "Management"
            :subtitle "Configure the users who will be using the system and their permissions"
            :users "Users"}
    :users {:username "Username"
            :fullname "Full Name"
            :email "Email"
            :admin "Admin"
            :password "Password"
            :password-confirmation "Password Confirmation"
            :invalid-credentials "Invalid username or password"
            :session-expired "Session expired, please login again"
            :password-hint "Leave blank if you don't want to change it"
            :unauthorized "Sorry, you are not allow to access this page. Please contact the administrator for more information."
            :basic-info "Basic Information"
            :permissions "Permissions"}
    :permissions {:allowed-cubes "Allowed Cubes"
                  :admin-all-cubes "Admin users view everything"
                  :all-cubes "Can view all cubes"
                  :no-cubes "Can view no cubes"
                  :only-cubes-selected "Can view only the following cubes"
                  :all-measures "All measures will be visible"
                  :only-measures-selected "Only the following measures will be visible"
                  :select-measures "Please select the allowed measures"
                  :no-measures "None"
                  :add-filter "Add Filter"}
    :account {:title "Your Account"
              :subtitle "Edit your profile details here"
              :current-password "Current Password"
              :new-password "New Password"
              :saved "Your account has been saved"
              :invalid-current-password "is incorrect"}
    :date-formats {:second "yyyy-MM-dd HH:mm:ss"
                   :minute "MMM d, h:mma"
                   :hour "MMM d, yyyy ha"
                   :day "MMM d, yyyy"
                   :month "MMM yyyy"}
    :calendar {:days ["S" "M" "T" "W" "T" "F" "S"]
               :months ["January" "February" "March" "April" "May" "June" "July" "August" "September" "October" "November" "December"]
               :monthsShort ["Jan" "Feb" "Mar" "Apr" "May" "Jun" "Jul" "Aug" "Sep" "Oct" "Nov" "Dec"]
               :today "Today"
               :now "Now"
               :am "AM"
               :pm "PM"}
    :actions {:ok "Accept"
              :cancel "Cancel"
              :edit "Modify"
              :save "Save"
              :save-as "Save As"
              :new "Create"
              :delete "Delete"
              :close "Close"
              :select "Select"
              :hold-delete "You must click the button and hold for one second to confirm"
              :search "Search"}
    :validation {:required "can't be blank"
                 :regex "doesn't match pattern"
                 :email "is not a valid email address"
                 :password "must have a combination of at least 7 letters and numbers (or symbols)"
                 :confirmation "doesn't match the previous value"}
    :boolean {:true "Yes"
              :false "No"}
    :errors {:no-results "No results where found"
             :no-desc "No description"
             :bad-gateway "The system is not available right now. Please try again later."
             "Query timeout" "The query is taking longer than expected. Please try with a shorter period."}}

   :es
   {:menu {:logout "Salir"}
    :home {:menu "Inicio"
           :title "Bienvenido!"
           :subtitle "Qué le gustaría analizar hoy?"}
    :cubes {:title "Cubos de Datos"
            :menu "Cubos"
            :missing "No hay cubos definidos"
            :data-range "Rango de datos disponibles"}
    :dashboards {:title "Dashboards"
                 :missing "No se ha creado ningún dashboard todavía"
                 :saved "Dashboard '{1}' guardado correctamente"
                 :deleted "Dashboard '{1}' eliminado correctamente"
                 :report-count #(cond
                                  (zero? %) "Sin reportes"
                                  (= 1 %) "1 reporte"
                                  :else "{1} reportes")
                 :no-reports "Este dashboard no tiene reportes"
                 :updated-at "Última actualización"}
    :reports {:title "Reportes"
              :missing "No se ha guardado ningún reporte todavía"
              :name "Nombre"
              :description "Descripción"
              :dashboards "Mostrar en Dashboards"
              :updated-at "Última actualización"
              :saved "Reporte '{1}' guardado correctamente"
              :deleted "Reporte '{1}' eliminado correctamente"
              :unauthorized "Oops! Este reporte ya no está disponible."}
    :viewer {:dimensions "Dimensiones"
             :measures "Métricas"
             :filters "Filtros"
             :split-on "Colocar En"
             :rows "Filas"
             :columns "Columnas"
             :limit "Límite"
             :sort-by "Ordenar Por"
             :granularity "Granularidad"
             :no-pinned "Arrastre dimensiones aquí para acceso rápido"
             :no-measures "Por favor seleccione al menos una métrica"
             :no-results "No se encontraron resultados que coincidan con los criterios especificados"
             :split-required "Se necesita al menos una dimensión en el split ver los datos en forma de {1}"
             :too-many-splits-for-chart "Para visualización de gráficos debe haber como máximo dos splits"
             :unauthorized "Oops! Parece que el cubo {1} ya no está disponible."
             :toggle-fullscreen "Mostrar en Pantalla Completa"}
    :viewer.period {:relative "Relativo"
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
                    :previous-year "Año Pasado"
                    :from "Desde"
                    :to "Hasta"}
    :viewer.operator {:include "Incluir"
                      :exclude "Excluir"}
    :viewer.viztype {:totals "totales"
                     :table "tabla"
                     :bar-chart "barras"
                     :line-chart "línea"
                     :pie-chart "torta"}
    :share {:title "Compartir"
            :copy-url "Copiar URL"
            :copied "URL Copiado!"}
    :raw-data {:menu "Ver datos desagregados"
               :title "Datos Desagregados"
               :showing "Primeros {1} eventos según filtro: "
               :button "Datos Desagregados"}
    :settings {:menu "Preferencias"
               :lang "Lenguaje"
               :update-now "Actualizar Ahora"
               :auto-refresh "Refrescar Automáticamente"
               :auto-refresh-opts (fn [] [["Nunca" 0] ["Cada 10 segundos" 10] ["Cada 30 segundos" 30] ["Cada 1 minuto" 60] ["Cada 10 minutos" 600] ["Cada 30 minutos" 1800]])
               :abbreviations "Abreviaturas de Números"
               :abbreviations-opts (fn [] [["User formato por defecto" "default"] ["No abreviar nada" "no"] ["Abreviar todo" "yes"]])}
    :admin {:menu "Configurar Usuarios"
            :title "Administración"
            :subtitle "Configure los usuarios que podrán acceder al sistema y sus permisos"
            :users "Usuarios"}
    :users {:username "Usuario"
            :fullname "Nombre"
            :email "Email"
            :password-confirmation "Confirmación de Password"
            :invalid-credentials "Usuario y/o password incorrecto"
            :session-expired "Sesión expirada, por favor ingrese nuevamente"
            :password-hint "Dejar en blanco para no cambiarlo"
            :unauthorized "Ud. no tiene acceso a esta página. Por favor, contacte al administrador para más información."
            :basic-info "Información Básica"
            :permissions "Permisos"}
    :permissions {:allowed-cubes "Cubes Visibles"
                  :admin-all-cubes "Administradores pueden ver todo"
                  :all-cubes "Puede ver todos los cubos"
                  :no-cubes "No puede ver ningún cubo"
                  :only-cubes-selected "Puede ver sólo los siguientes cubos"
                  :all-measures "Todas las métricas serán visibles"
                  :only-measures-selected "Sólo las siguientes métricas serán visibles"
                  :select-measures "Por favor seleccion las métricas permitidas"
                  :no-measures "Ninguna"
                  :add-filter "Agregar Filtro"}
    :account {:title "Tu Cuenta"
              :subtitle "Aquí puede cambiar los detalles de su perfil"
              :current-password "Password Actual"
              :new-password "Nuevo Password"
              :saved "Tu cuenta se grabó correctamente"
              :invalid-current-password "es incorrecto"}
    :date-formats {:second "dd/MM/yyyy HH:mm:ss"
                   :minute "dd/MM HH:mm"
                   :hour "dd/MM/yy H 'hs'"
                   :day "dd/MM/yy"
                   :month "MM/yy"}
    :calendar {:days ["D" "L" "M" "M" "J" "V" "S"]
               :months ["Enero" "Febrero" "Marzo" "Abril" "Mayo" "Junio" "Julio" "Agosto" "Septiembre" "Octubre" "Noviembre" "Diciembre"]
               :monthsShort ["Ene" "Feb" "Mar" "Abr" "May" "Jun" "Jul" "Ago" "Sep" "Oct" "Nov" "Dic"]
               :today "Hoy"
               :now "Ahora"}
    :actions {:ok "Aceptar"
              :cancel "Cancelar"
              :edit "Editar"
              :save "Guardar"
              :save-as "Guardar Como"
              :new "Crear"
              :delete "Eliminar"
              :close "Cerrar"
              :select "Seleccionar"
              :hold-delete "Clickee el botón y mantenga presionado por un segundo para confirmar"
              :search "Buscar"}
    :validation {:required "este campo es obligatorio"
                 :email "no es una dirección válida"
                 :password "debería tener al menos 7 letras y números (o símbolos)"
                 :confirmation "no coincide con el valor anterior"}
    :boolean {:true "Si"
              :false "No"}
    :errors {:no-results "No se encontraron resultados"
             :no-desc "Sin descripción"
             :bad-gateway "El sistema no está disponible en este momento. Por favor, intente nuevamente más tarde."
             "Query timeout" "La consulta está demorando demasiado. Por favor, intente con un período más corto."}}

   :tongue/fallback :en})

(def translate (tongue/build-translate translations))

(defn lang []
  (keyword (db/get-in [:settings :lang] "en")))

(defn t [& args]
  (apply translate (lang) args))

(defn translation [& args]
  (get-in translations (into [(lang)] args)))

(defn translation! [& args]
  (or (apply translation args)
      (apply str "{Missing key " args "}")))
