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
            [shevek.viewer.filter :refer [filter-popup build-filter empty-value?]]
            [shevek.components.popup :refer [show-popup]]))

(defn filter-button* [cube dim]
  (let [remove-filter #(swap! cube update :filters remove-dimension dim)
        update-filter #(if (empty-value? %2)
                         (remove-filter)
                         (swap! cube update :filters replace-dimension (build-filter dim %2)))
        remove-if-empty #(let [updated-dim (find-dimension (:name dim) (:filters @cube))]
                           (when (empty-value? updated-dim) (remove-filter)))]
    [:a.ui.tiny.right.labeled.icon.button
     {:on-click #(show-popup % ^{:key (select-keys dim [:name :added])}
                             [filter-popup dim {:cube (:name @cube) :on-filter-change update-filter}]
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

(defn- cube-permissions [user _ i]
  (let [cube (r/cursor user [:cubes i])]
    (fn [_ {:keys [title description selected only-measures-selected measures allowed-measures filters dimensions]} _]
      [:div.item {:on-click #(swap! cube update :selected not)
                  :class (when-not selected "hidden")}
       [:i.icon.large.selected {:class (if selected "checkmark" "minus")}]
       [:div.content
        [:div.header title]
        [:div.description [:p description]
         (when selected
           [:div {:on-click #(.stopPropagation %)}
            [:div.measures
             [:p [:a {:on-click #(swap! cube update :only-measures-selected not)}
                  (t (if only-measures-selected :permissions/only-measures-selected :permissions/all-measures))]]
             (when only-measures-selected
               [select (map (juxt :title :name) measures)
                {:class "multiple fluid search selection"
                 :selected allowed-measures
                 :on-change #(swap! cube assoc :allowed-measures (split % #","))
                 :placeholder (t :permissions/select-measures)}])]
            [:div.filters
             [dropdown (map (juxt :title :name) (remove #(includes-dim? filters %) dimensions))
              {:class "labeled icon top left pointing tiny basic button" :in-menu-search true
               :on-change #(swap! cube update :filters conj (to-filter (find-dimension % dimensions)))}
              [:i.filter.icon]
              [:span (if (seq filters) (str (t :viewer/filters) ":") (t :permissions/add-filter))]]
             (when (seq filters)
               [:span
                (for [{:keys [name] :as dim} filters]
                  ^{:key name} [filter-button cube dim])])]])]]])))

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
