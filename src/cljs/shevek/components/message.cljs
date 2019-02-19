(ns shevek.components.message)

(defn page-message [{:keys [header content]}]
  [:div.ui.container.page-message
   [:div.ui.negative.icon.message
    [:i.exclamation.triangle.icon]
    [:div.content
     [:div.header header]
     content]]])

(defn warning [text]
  [:div.icon-hint
   [:i.warning.circle.icon]
   [:div.text text]])
