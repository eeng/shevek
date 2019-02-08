(ns shevek.pages.configuration.users.list
  (:require [reagent.core :as r]
            [cuerdas.core :as str]
            [shevek.i18n :refer [t]]
            [shevek.components.text :refer [mail-to]]
            [shevek.components.form :refer [hold-to-confirm search-input filter-matching by]]
            [shevek.components.notification :refer [notify]]
            [shevek.components.confirmation :refer [with-confirm]]
            [shevek.rpc :as rpc]
            [shevek.reflow.db :as db]
            [shevek.reflow.core :refer [dispatch] :refer-macros [defevh]]
            [shevek.lib.collections :refer [find-by]]
            [shevek.pages.configuration.users.form :refer [adapt-for-client user-form]]
            [shevek.pages.designer.actions.raw :refer [filters->str]]
            [shevek.pages.profile.helpers :refer [avatar]]))

(defevh :users/fetch [db]
  (rpc/fetch db :users "users/find-all"))

(def default-user {:admin false})

(defevh :users/delete [db id]
  (rpc/call "users/delete" :args [id] :handler #(do
                                                  (notify (t :users/deleted))
                                                  (dispatch :users/fetch %))))

(defn- cube-permissions-text [{:keys [name measures filters] :or {measures "all"}}]
  (let [cube-title (db/get-in [:cubes name :title])
        all-measures (db/get-in [:cubes name :measures])
        measures-titles (if (seq measures)
                          (->> measures
                               (map #(:title (find-by :name % all-measures)))
                               (str/join ", "))
                          (t :permissions/no-measures))
        filters-details (when (seq filters)
                          (->> filters
                               (map #(merge (find-by :name (:name %) (db/get-in [:cubes name :dimensions])) %))
                               filters->str))
        measures-details (when (not= measures "all")
                           (str (t :designer/measures) ": " measures-titles))
        details (->> [filters-details measures-details] (filter some?) (interpose ", "))]
    [:span.cube
     [:span.cube-name cube-title]
     (when (seq details) [:span.cube-details " [" (into [:span] details) "]"])]))

(defn permissions-text [{:keys [allowed-cubes] :or {allowed-cubes "all"}}]
  (cond
    (= allowed-cubes "all") [:div.permissions (t :permissions/all-cubes)]
    (empty? allowed-cubes) [:div.permissions (t :permissions/no-cubes)]
    :else (into [:div.permissions (str (t :permissions/only-cubes-selected) ": ")]
                (->> allowed-cubes (map cube-permissions-text) (interpose ", ")))))

(defn- user-row [{:keys [username fullname email admin id] :as user} edited-user]
  [:tr
   [:td [avatar user]]
   [:td username]
   [:td fullname]
   [:td (when (seq email) (mail-to email))]
   [:td (if admin
          [:div.extra
           [:div.ui.blue.label "Admin"]]
          (permissions-text user))]
   [:td.actions
    [:button.ui.inverted.compact.circular.secondary.icon.button
     {:on-click #(reset! edited-user (adapt-for-client user))
      :data-tid "edit"}
     [:i.edit.icon]]
    [with-confirm
     [:button.ui.inverted.compact.circular.red.icon.button
      {:on-click #(dispatch :users/delete id)}
      [:i.trash.icon]]]]])

(defn- users-list [edited-user]
  (r/with-let [search (r/atom "")]
    [:div.ui.grid.users-list
     [:div.five.wide.column
      [search-input search {:placeholder (t :users/search-hint)}]]
     [:div.eleven.wide.column.right.aligned
      [:button.ui.green.button
       {:on-click #(reset! edited-user (adapt-for-client default-user))}
       [:i.plus.icon]
       (t :actions/new)]]
     [:div.sixteen.wide.column
      (let [users (filter-matching @search (by :username :fullname) (db/get :users))]
        (if (seq users)
          [:table.ui.striped.table
           [:thead>tr
            [:th]
            [:th (t :users/username)]
            [:th (t :users/fullname)]
            [:th (t :users/email)]
            [:th (t :users/permissions)]
            [:th.center.aligned.collapsing (t :actions/header)]]
           [:tbody
            (for [user users]
              ^{:key (:username user)} [user-row user edited-user])]]
          [:div.large.tip (t :errors/no-results)]))]]))

(defn users-section []
  (dispatch :cubes/fetch)
  (dispatch :users/fetch)
  (let [edited-user (r/atom nil)]
    (fn []
      (if @edited-user
        [user-form edited-user]
        [users-list edited-user]))))
