(ns shevek.components.refresh
  (:require [reagent.core :as r]
            [shevek.i18n :refer [t]]
            [shevek.reflow.db :as db]
            [shevek.reflow.core :refer [dispatch] :refer-macros [defevh]]
            [shevek.components.popup :refer [show-popup close-popup tooltip]]
            [shevek.components.form :refer [select]]
            [cuerdas.core :as str]))

(defonce auto-refresh-interval (atom {}))

(defn- clear-auto-refresh-interval! []
  (js/clearInterval (:id @auto-refresh-interval)))

(defn- set-auto-refresh-interval! [{:keys [every on-refresh]}]
  (clear-auto-refresh-interval!)
  (when (and every (pos? every))
    (reset! auto-refresh-interval {:id (js/setInterval on-refresh (* 1000 every))
                                   :every every
                                   :on-refresh on-refresh})))

(defn debounce-auto-refresh! []
  (set-auto-refresh-interval! @auto-refresh-interval))

(defn- popup-content [every {:keys [on-refresh loading?]}]
  [:div#settings-popup.ui.form
   [:div.field
    [:label (t :preferences/refresh-every)]
    [select (t :preferences/refresh-every-opts)
     {:selected every
      :on-change #(let [new-every (str/parse-int %)]
                    (set-auto-refresh-interval! {:every new-every :on-refresh on-refresh})
                    (dispatch :preferences/save {:auto-refresh new-every})
                    (close-popup))}]]
   [:button.ui.fluid.primary.button
    {:on-click on-refresh
     :class (when (loading?) "loading disabled")}
    (t :preferences/refresh-now)]])

(defn refresh-button [{:keys [on-refresh loading?] :or {loading? (constantly false)}}]
  (r/with-let [_ (set-auto-refresh-interval! {:every (db/get-in [:preferences :auto-refresh])
                                              :on-refresh on-refresh})]
    (let [every (db/get-in [:preferences :auto-refresh])] ; Repetead to get the updated value when the user changes it
      [:button.ui.default.icon.button
       {:on-click #(show-popup %
                               [popup-content every {:on-refresh on-refresh :loading? loading?}]
                               {:position "bottom right"})}
       (when (pos? every)
         [:span.current-refresh
           (t :preferences/refresh-every) " "
           (get (t :preferences/refresh-every-opts) every)])
       [:i.refresh.icon {:class (when (loading?) "loading")}]])
    (finally
      (clear-auto-refresh-interval!))))
