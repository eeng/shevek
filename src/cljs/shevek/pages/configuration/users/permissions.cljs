(ns shevek.pages.configuration.users.permissions
  (:require [reagent.core :as r]
            [shevek.i18n :refer [t]]
            [shevek.lib.react :refer [rmap without-propagation]]
            [shevek.lib.string :refer [split]]
            [shevek.domain.dimension :refer [includes-dim? find-dimension remove-dimension replace-dimension time-dimension?]]
            [shevek.components.form :refer [select dropdown]]
            [shevek.pages.designer.filters :refer [filter-title filter-popup build-filter empty-value? show-popup-when-added set-as-last-added-filter]]
            [shevek.components.popup :refer [show-popup]]))

(defn filter-button [cube {:keys [name]}]
  (let [popup-key (hash {:name name :timestamp (js/Date.)})]
    (fn [cube dim]
      (let [remove-filter #(swap! cube update :filters remove-dimension dim)
            update-filter #(if (empty-value? %2)
                             (remove-filter)
                             (swap! cube update :filters replace-dimension (build-filter dim %2)))
            remove-if-empty #(let [updated-dim (find-dimension name (:filters @cube))]
                               (when (empty-value? updated-dim) (remove-filter)))]
        [:a.ui.tiny.right.labeled.icon.button
         {:on-click #(show-popup % ^{:key popup-key}
                                 [filter-popup dim {:cube (:name @cube)
                                                    :on-filter-change update-filter
                                                    :time-filter {:period "latest-30days"}}]
                                 {:position "bottom center" :on-close remove-if-empty})
          :ref (partial show-popup-when-added name)}
         (filter-title dim)
         [:i.delete.icon {:on-click (without-propagation remove-filter)}]]))))

(defn- to-filter [dim]
  (let [opts (if (time-dimension? dim)
               {:period "latest-day"}
               {:operator "include" :value #{}})]
    (build-filter dim opts)))

(defn- cube-permissions [user {:keys [dimensions]} i]
  (let [cube (r/cursor user [:cubes i])
        add-filter (fn [name]
                     (set-as-last-added-filter name)
                     (swap! cube update :filters conj (to-filter (find-dimension name dimensions))))]
    (fn [_ {:keys [title description selected only-measures-selected measures allowed-measures filters dimensions]} _]
      [:div.item {:on-click #(swap! cube update :selected not)
                  :class (when-not selected "hidden")}
       [:i.icon.large.selected {:class (if selected "check" "close")}]
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
             [dropdown (map (juxt :title :name) (remove #(includes-dim? filters %) (sort-by :title dimensions)))
              {:class "labeled icon top left pointing tiny basic button" :in-menu-search true :on-change add-filter}
              [:i.filter.icon]
              [:span (if (seq filters) (str (t :designer/filters) ":") (t :permissions/add-filter))]]
             (when (seq filters)
               [:span
                (for [{:keys [name] :as dim} filters]
                  ^{:key name} [filter-button cube dim])])]])]]])))

(defn user-permissions [user]
  (let [{:keys [only-cubes-selected cubes]} @user]
    [:div.permissions-fields
     [:h3.ui.orange.header (t :users/permissions)]
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
