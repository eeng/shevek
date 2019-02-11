(ns shevek.components.layout)

(defn page-with-header [{:keys [title subtitle icon id image]} & children]
  [:div {:id id}
   [:div.ui.inverted.basic.segment.page-title
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

(defn topbar [{:keys [left right]}]
  [:div.topbar
   [:div.left left]
   [:div.right right
    (when (and (not left) (not right))
      [:button.ui.button.placeholder])]])

(defn simple-bar [content]
  [:div.scrollable {:data-simplebar true}
   [:div.simplebar-wrapper
    [:div.simplebar-height-auto-observer-wrapper
     [:div.simplebar-height-auto-observer]]
    [:div.simplebar-mask
     [:div.simplebar-offset
      [:div.simplebar-content content]]]
    [:div.simplebar-placeholder]]
   [:div.simplebar-track.simplebar-horizontal
    [:div.simplebar-scrollbar]]
   [:div.simplebar-track.simplebar-vertical
    [:div.simplebar-scrollbar]]])

(defn panel [{:keys [title actions scrollable]} & content]
  (let [panel-content (into [:div.panel-content] content)]
    [:div.ui.segment.clearing.panel
     [:div.ui.top.attached.label.panel-header
      title
      (when (seq actions)
        (into [:div.panel-actions] actions))]
     (if scrollable
       [simple-bar panel-content]
       panel-content)]))
