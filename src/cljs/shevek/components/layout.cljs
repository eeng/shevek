(ns shevek.components.layout)

(defn page-with-header [{:keys [title subtitle icon id image]} & children]
  [:div {:id id}
   [:div.ui.blue.inverted.basic.segment.page-title
    [:div.ui.container
     [:h1.ui.inverted.header
      (when icon [:i.icon {:class icon}])
      image
      [:div.content title
       [:div.sub.header subtitle]]]]]
   (into [:div.ui.container] children)
   [:div.page-footer]])

(defn page-loader []
  [:div.ui.active.large.loader])

(defn panel [{:keys [title]} & content]
  (into
   [:div.ui.segment.clearing.panel
    [:div.ui.top.attached.label title]]
   content))
