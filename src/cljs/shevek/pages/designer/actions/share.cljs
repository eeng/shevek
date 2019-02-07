(ns shevek.pages.designer.actions.share
  (:require [reagent.core :as r]
            [shevek.i18n :refer [t]]
            [shevek.components.popup :refer [tooltip]]
            [shevek.components.modal :refer [show-modal close-modal]]
            [shevek.components.notification :refer [notify]]
            [shevek.rpc :as rpc]
            [cljsjs.clipboard]))

(defn clipboard-button [button]
  (let [clipboard-atom (atom nil)]
    (r/create-class
     {:component-did-mount #(let [clipboard (new js/Clipboard (r/dom-node %))]
                              (reset! clipboard-atom clipboard))
      :component-will-unmount #(when @clipboard-atom
                                 (.destroy @clipboard-atom)
                                 (reset! clipboard-atom nil))
      :reagent-render (fn [] button)})))

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
           [:input#link {:type "text" :read-only true :value @url}]
           [clipboard-button
            [:button.ui.green.button
             {:data-clipboard-target "#link"
              :on-click #(do
                           (notify (t :share/copied))
                           (js/setTimeout close-modal 100))}
             (t :share/copy)]]]]]
        [:div.tip.top.spaced (t :share/report-hint)]]])))

(defn share-button [report]
  [:button.ui.default.icon.button
   {:ref (tooltip (t :share/title))
    :on-click #(show-modal [share-dialog report])}
   [:i.share.alternate.icon]])
