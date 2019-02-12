(ns shevek.pages.designer.actions.share
  (:require [reagent.core :as r]
            [shevek.i18n :refer [t]]
            [shevek.components.popup :refer [tooltip]]
            [shevek.components.modal :refer [show-modal close-modal]]
            [shevek.components.notification :refer [notify]]
            [shevek.components.clipboard :refer [clipboard-button]]
            [shevek.rpc :as rpc]))

(defn generate-share-url [report url]
  (rpc/call "reports/share-url" :args [report] :handler #(reset! url %)))

(defn- share-dialog [report]
  (let [url (r/atom "")]
    (generate-share-url report url)
    (fn []
      [:div.ui.tiny.modal
       [:div.header (t :share/title)]
       [:div.content
        [:div.ui.form {:class (when (empty? @url) "loading")}
         [:div.field
          [:label (t :share/label)]
          [:div.ui.action.input
           [:input#link {:type "text" :read-only true :value @url :on-focus #(-> % .-target .select)}]
           [clipboard-button
            [:button.ui.green.button
             {:data-clipboard-target "#link"
              :on-click #(do
                           (notify (t :share/copied))
                           (js/setTimeout close-modal 100))}
             (t :share/copy)]]]]]
        [:div.tip.top.spaced (t :reports/share-hint)]]])))

(defn share-button [report]
  [:button.ui.default.icon.button
   {:on-click #(show-modal [share-dialog report] {:autofocus false})
    :ref (tooltip (t :share/title))
    :data-tid "share"}
   [:i.share.alternate.icon]])
