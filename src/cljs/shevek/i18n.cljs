(ns shevek.i18n
  (:require [tongue.core :as tongue]
            [shevek.reflow.db :as db]))

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
            :no-pinned "Drag dimensions here to pin them for quick access"
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
              :none "Save your favorite reports with the \"Pin in dashboard\" option so they appear here."
              :hold-delete "Hold for {1} seconds to delete this report"}
    :share {:title "Share"}
    :raw-data {:menu "View raw data"
               :title "Raw Event Data"
               :showing "Showing the first {1} events matching: "
               :button "Raw Data"}
    :settings {:menu "Settings"
               :lang "Language"
               :update-now "Update Now"
               :auto-refresh "Auto Update"
               :auto-refresh-opts (fn [] [["Off" 0] ["Every 10 seconds" 10] ["Every 30 seconds" 30] ["Every minute" 60] ["Every 10 minutes" 600] ["Every 30 minutes" 1800]])}
    :admin {:menu "Manage"
            :title "Management"
            :subtitle "Configure the users, cube descriptions and other stuff in this page"
            :users "Users"}
    :users {:username "Username"
            :fullname "Full Name"
            :email "Email"
            :password "Password"
            :password-confirmation "Password Confirmation"
            :invalid-credentials "Invalid username or password"
            :session-expired "Session expired, please login again"
            :password-hint "Leave blank if you don't want to change it"}
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
    :actions {:ok "Accept"
              :cancel "Cancel"
              :edit "Modify"
              :save "Save"
              :save-as "Save As"
              :new "New"
              :delete "Delete"
              :close "Close"
              :select "Select"}
    :input {:search "Search"}
    :validation {:required "can't be blank"
                 :regex "doesn't match pattern"
                 :email "is not a valid email address"
                 :password "must have a combination of at least 7 letters and numbers (or symbols)"
                 :confirmation "doesn't match the previous value"}
    :boolean {:true "Yes"
              :false "No"}}

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
            :no-pinned "Arrastre dimensiones aquí para acceso rápido"
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
              :none "Guardá tus reportes favoritos con la opción 'Mostrar en dashboard' para que aparezcan aquí."
              :hold-delete "Clickee y mantenga presionado por {1} segundos para eliminar el reporte"}
    :share {:title "Compartir"}
    :raw-data {:menu "Ver datos desagregados"
               :title "Datos Desagregados"
               :showing "Primeros {1} eventos según filtro: "
               :button "Datos Desagregados"}
    :settings {:menu "Preferencias"
               :lang "Lenguaje"
               :update-now "Actualizar Ahora"
               :auto-refresh "Refrescar Automáticamente"
               :auto-refresh-opts (fn [] [["Nunca" 0] ["Cada 10 segundos" 10] ["Cada 30 segundos" 30] ["Cada 1 minuto" 60] ["Cada 10 minutos" 600] ["Cada 30 minutos" 1800]])}
    :admin {:menu "Configurar"
            :title "Administración"
            :subtitle "Configure los usuarios, descripciones de cubos y otras cuestiones aquí"
            :users "Usuarios"}
    :users {:username "Usuario"
            :fullname "Nombre"
            :email "Email"
            :password-confirmation "Confirmación de Password"
            :invalid-credentials "Usuario y/o password incorrecto"
            :session-expired "Sesión expirada, por favor ingrese nuevamente"
            :password-hint "Dejar en blanco para no cambiarlo"}
    :account {:title "Tu Cuenta"
              :subtitle "Aquí podés cambiar los detalles de tu perfil"
              :current-password "Password Actual"
              :new-password "Nuevo Password"
              :saved "Tu cuenta se grabó correctamente"
              :invalid-current-password "es incorrecto"}
    :date-formats {:second "dd/MM/yyyy HH:mm:ss"
                   :minute "dd/MM HH:mm"
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
              :close "Cerrar"
              :select "Seleccionar"}
    :input {:search "Buscar"}
    :validation {:required "este campo es obligatorio"
                 :email "no es una dirección válida"
                 :password "debería tener al menos 7 letras y números (o símbolos)"
                 :confirmation "no coincide con el valor anterior"}
    :boolean {:true "Si"
              :false "No"}}

   :tongue/fallback :en})

(def translate (tongue/build-translate translations))

(defn lang []
  (keyword (db/get-in [:settings :lang] "en")))

(defn t [& args]
  (apply translate (lang) args))

(defn translation [& args]
  (get-in translations (into [(lang)] args)))
