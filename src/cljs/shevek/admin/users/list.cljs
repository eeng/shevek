(ns shevek.admin.users.list
  (:require [reagent.core :as r]
            [cuerdas.core :as str]
            [shevek.i18n :refer [t]]
            [shevek.components.text :refer [mail-to]]
            [shevek.components.form :refer [hold-to-confirm search-input filter-matching by]]
            [shevek.rpc :as rpc]
            [shevek.reflow.db :as db]
            [shevek.reflow.core :refer [dispatch] :refer-macros [defevh]]
            [shevek.lib.util :refer [new-record?]]
            [shevek.lib.collections :refer [find-by]]
            [shevek.admin.users.form :refer [adapt-for-client user-form]]))

(defevh :users-requested [db]
  (rpc/fetch db :users "users/find-all"))

(def default-user {:admin false})

(defevh :user-deleted [db user]
  (rpc/call "users/delete" :args [user] :handler #(dispatch :users-requested %)))

(defn- user-item [{:keys [username fullname email admin allowed-cubes] :or {allowed-cubes "all"} :as original-user} edited-user]
  (let [cube-permissions (fn [{:keys [name measures]}]
                           (let [cube-title (db/get-in [:cubes name :title])
                                 all-measures (db/get-in [:cubes name :measures])
                                 measures-titles (if (seq measures)
                                                   (->> measures
                                                        (map #(:title (find-by :name % all-measures)))
                                                        (str/join ", "))
                                                   (t :permissions/no-measures))
                                 measures-details (when (not= measures "all")
                                                    (str " (" (t :viewer/measures) ": " measures-titles ")"))]
                             (str cube-title measures-details)))
        allowed-cubes-text
        (cond
          (= allowed-cubes "all") (t :permissions/all-cubes)
          (empty? allowed-cubes) (t :permissions/no-cubes)
          :else (str (t :permissions/only-cubes-selected) ": "
                     (->> allowed-cubes (map cube-permissions) (str/join ", "))))]
    [:div.item
     [:div [:i.user.huge.icon]]
     [:div.content
      [:div.right.floated
       [:button.ui.compact.basic.button
        {:on-click #(reset! edited-user (adapt-for-client original-user))}
        (t :actions/edit)]
       [:button.ui.compact.basic.red.button
        (hold-to-confirm #(dispatch :user-deleted original-user))
        (t :actions/delete)]]
      [:div.header fullname]
      [:div.meta username (when (seq email) [:span " | " (mail-to email)])]
      (if admin
        [:div.extra
         [:div.ui.blue.label "Admin"]]
        [:div.description allowed-cubes-text])]]))

(defn- users-list [edited-user]
  (let [search (r/atom "")]
    (fn []
      [:div.users-list
       [search-input search {:wrapper {:class "big"}}]
       [:div.ui.padded.segment
        [:div.ui.divided.items
         (let [users (filter-matching @search (by :username :fullname) (db/get :users))]
           (if (seq users)
             (for [user users]
               ^{:key (:username user)} [user-item user edited-user])
             [:div.large.tip (t :errors/no-results)]))]]])))

(defn users-section []
  (dispatch :users-requested)
  (let [edited-user (r/atom nil)]
    (fn []
      [:section.users
       [:button.ui.button.right.floated
        {:on-click #(reset! edited-user (adapt-for-client default-user))}
        (t :actions/new)]
       [:h2.ui.app.header (t :admin/users)]
       (if @edited-user
         [user-form edited-user]
         [users-list edited-user])])))
