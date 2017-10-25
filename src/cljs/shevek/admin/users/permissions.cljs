(ns shevek.admin.users.permissions
  (:require [shevek.i18n :refer [t]]
            [shevek.lib.string :refer [split]]
            [shevek.components.form :refer [select]]))

(defn- cube-permissions [user {:keys [title description selected measures only-measures-selected allowed-measures] :as cube} i]
  [:div.item {:on-click #(swap! user update-in [:cubes i :selected] not)
              :class (when-not selected "hidden")}
   [:i.icon.large.selected {:class (if selected "checkmark" "minus")}]
   [:div.content
    [:div.header title]
    [:div.description [:p description]
     (when selected
       [:div.measures {:on-click #(.stopPropagation %)}
        [:a {:on-click #(swap! user update-in [:cubes i :only-measures-selected] not)}
         (t (if only-measures-selected :permissions/only-measures-selected :permissions/all-measures))]
        (when only-measures-selected
          [select (map (juxt :title :name) measures)
           {:class "multiple fluid search selection"
            :selected allowed-measures
            :on-change #(swap! user assoc-in [:cubes i :allowed-measures] (split % #","))
            :placeholder (t :permissions/select-measures)}])])]]])

(defn user-permissions [user]
  (let [{:keys [only-cubes-selected cubes]} @user]
    [:div.permissions-fields
     [:h3.ui.header (t :users/permissions)]
     [:h4.ui.header (t :permissions/allowed-cubes)]
     (if (:admin @user)
       [:div (t :permissions/admin-all-cubes)]
       [:div
        [:a {:on-click #(swap! user update :only-cubes-selected not)}
         (t (if only-cubes-selected :permissions/only-cubes-selected :permissions/all-cubes))]
        (when only-cubes-selected
          [:div.ui.divided.items
           (for [[i {:keys [name] :as cube}] (map-indexed vector cubes)]
             ^{:key name} [cube-permissions user cube i])])])]))
