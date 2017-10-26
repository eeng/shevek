(ns shevek.admin.users.permissions
  (:require [reagent.core :as r]
            [shevek.i18n :refer [t]]
            [shevek.lib.react :refer [rmap without-propagation]]
            [shevek.lib.util :refer [trigger-click]]
            [shevek.lib.string :refer [split]]
            [shevek.lib.dw.dims :refer [includes-dim? find-dimension remove-dimension replace-dimension time-dimension?]]
            [shevek.reflow.core :refer-macros [defevh]]
            [shevek.components.form :refer [select dropdown]]
            [shevek.viewer.shared :refer [filter-title]]
            [shevek.viewer.filter :refer [filter-popup build-filter]]
            [shevek.components.popup :refer [show-popup]]))

(defn filter-button* [user cube-idx dim]
  (let [cube (get-in @user [:cubes cube-idx :name])
        remove-filter #(swap! user update-in [:cubes cube-idx :filters] remove-dimension dim)
        update-filter #(if (seq (:value %2))
                         (swap! user update-in [:cubes cube-idx :filters] replace-dimension (build-filter dim %2))
                         (remove-filter))
        remove-if-empty #(let [new-value (:value (find-dimension (:name dim) (get-in @user [:cubes cube-idx :filters])))]
                           (when (and (not (time-dimension? dim)) (empty? new-value))
                             (remove-filter)))]
    [:a.ui.tiny.right.labeled.icon.button
     {:on-click #(show-popup % ^{:key (select-keys dim [:name :added])}
                             [filter-popup dim {:cube cube :on-filter-change update-filter}]
                             {:position "bottom center" :on-close remove-if-empty})}
     (filter-title dim)
     [:i.delete.icon {:on-click (without-propagation remove-filter)}]]))

(def filter-button
  (with-meta filter-button* {:component-did-mount #(-> % r/dom-node trigger-click)}))

(defn- to-filter [dim]
  (let [opts (if (time-dimension? dim)
               {:period "latest-day"}
               {:operator "include" :value #{}})]
    (build-filter dim (assoc opts :added (js/Date.)))))

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
           :on-change #(swap! user update-in [:cubes i :filters] conj (to-filter (find-dimension % dimensions)))}
          [:i.filter.icon]
          [:span (if (seq filters) (str (t :viewer/filters) ":") (t :permissions/add-filter))]]
         (when (seq filters)
           [:span
            (for [{:keys [name] :as dim} filters]
              ^{:key name} [filter-button user i dim])])]])]]])

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
