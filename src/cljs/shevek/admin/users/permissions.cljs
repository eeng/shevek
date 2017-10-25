(ns shevek.admin.users.permissions
  (:require [shevek.i18n :refer [t]]
            [shevek.lib.react :refer [rmap without-propagation]]
            [shevek.lib.string :refer [split]]
            [shevek.lib.dw.dims :refer [includes-dim? find-dimension remove-dimension time-dimension?]]
            [shevek.reflow.core :refer-macros [defevh]]
            [shevek.components.form :refer [select dropdown]]
            [shevek.viewer.shared :refer [filter-title]]
            [shevek.viewer.filter :refer [filter-popup]]
            [shevek.components.popup :refer [show-popup]]))

(defn- filter-button [user cube-idx {:keys [name] :as dim}]
  [:a.ui.label
   {:on-click #(show-popup % ^{:key name} [filter-popup dim {:cube (get-in @user [:cubes cube-idx :name])}]
                           {:position "right center"})}
   (filter-title dim)
   [:i.delete.icon {:on-click (without-propagation swap! user update-in [:cubes cube-idx :filters] remove-dimension dim)}]])

(defn- build-filter [dim]
  (if (time-dimension? dim)
    (assoc dim :period "latest-day")
    (assoc dim :operator "include" :value #{})))

(defn- cube-permissions [user {:keys [title description selected
                                      only-measures-selected measures allowed-measures
                                      filters dimensions]
                               :as cube} i]
  [:div.item {:on-click #(swap! user update-in [:cubes i :selected] not)
              :class (when-not selected "hidden")}
   [:i.icon.large.selected {:class (if selected "checkmark" "minus")}]
   [:div.content
    [:div.header title]
    [:div.description [:p description]
     (when selected
       [:div {:on-click #(.stopPropagation %)}
        [:div.measures
         [:p [:a {:on-click #(swap! user update-in [:cubes i :only-measures-selected] not)}
              (t (if only-measures-selected :permissions/only-measures-selected :permissions/all-measures))]]
         (when only-measures-selected
           [select (map (juxt :title :name) measures)
            {:class "multiple fluid search selection"
             :selected allowed-measures
             :on-change #(swap! user assoc-in [:cubes i :allowed-measures] (split % #","))
             :placeholder (t :permissions/select-measures)}])]
        [:div.filters
         [dropdown (map (juxt :title :name) (remove #(includes-dim? filters %) dimensions))
          {:class "labeled icon top left pointing tiny basic button" :in-menu-search true
           :on-change #(swap! user update-in [:cubes i :filters] conj (build-filter (find-dimension % dimensions)))}
          [:i.filter.icon]
          [:span (if (seq filters) (str (t :viewer/filters) ":") (t :permissions/add-filter))]]
         (when (seq filters)
           [:span (rmap (partial filter-button user i) :name filters)])]])]]])

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
