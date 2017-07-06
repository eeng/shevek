(ns shevek.web.pages
  (:require [hiccup.page :refer [html5 include-js include-css]]
            [hiccup.form :refer [form-to text-field password-field submit-button]]))

(defn layout [& body]
  (html5
   [:head
    [:title "Shevek"]
    [:link {:rel "shortcut icon" :href "favicon.ico"}]
    (include-css "css/semantic.min.css" "css/app.css")
    (include-js "js/jquery.min.js" "js/semantic.min.js")]
   (into [:body] body)))

(defn index []
  (layout
   [:div#app]
   (include-js "js/app.js")))

(defn login [{:keys [username]}]
  (layout
   [:div#login.ui.center.aligned.grid
    [:div.column
     [:h1.ui.blue.header
      [:i.cubes.icon]
      [:div.content "Shevek"
       [:div.sub.header "Data Warehouse Visualization System"]]]
     (form-to
      {:class (str "ui large form " (when username "error"))} [:post "/login"]
      [:div.ui.stacked.segment
       [:div.field
        [:div.ui.left.icon.input
         [:i.user.icon]
         (text-field {:placeholder "User" :autofocus true} "username" username)]]
       [:div.field
        [:div.ui.left.icon.input
         [:i.lock.icon]
         (password-field {:placeholder "Password"} "password")]]
       (submit-button {:class "ui fluid large blue submit button"} "Login")]
      [:div.ui.error.message "Usuario y/o password incorrecto."])]]))

#_(println (index))
#_(println (login {:username "emma"}))
