(ns shevek.components.layout
  (:require [reagent.core :as r]))

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

(defn perfect-scrollbar []
  (let [ps (r/atom nil)]
    (r/create-class
     {:display-name "perfect-scrollbar"
      :component-did-mount #(reset! ps (-> % r/dom-node js/PerfectScrollbar.))
      :component-will-update #(.update @ps)
      :component-will-unmount #(.destroy @ps)
      :reagent-render (fn [child] [:div.scrollable child])})))

(defn panel [{:keys [title actions scrollable]} & content]
  (let [panel-content (into [:div.panel-content] content)]
    [:div.ui.segment.clearing.panel
     [:div.ui.top.attached.label.panel-header
      title
      (when (seq actions)
        (into [:div.panel-actions] actions))]
     (if scrollable
       [perfect-scrollbar panel-content]
       panel-content)]))
