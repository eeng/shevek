(ns shevek.menu.share
  (:require [reagent.core :as r]
            [cljsjs.clipboard]
            [cljsjs.filesaverjs]
            [shevek.i18n :refer [t]]
            [shevek.components.popup :refer [show-popup close-popup]]
            [shevek.reflow.core :refer [dispatch] :refer-macros [defevh]]
            [shevek.lib.notification :refer [notify]]
            [shevek.rpc :as rpc]
            [testdouble.cljs.csv :as csv]))

(defn download [filename content & [mime-type]]
  (let [mime-type (or mime-type (str "text/plain;charset=" (.-characterSet js/document)))
        blob (new js/Blob
                  (clj->js [content])
                  (clj->js {:type mime-type}))]
    (js/saveAs blob filename)))

(defevh :viewer/export-as-xls [db]
  (download "test.csv"
            (csv/write-csv [[1 2 3] [4 5 7] ["ñoqui" "avión" ""]] :newline :cr+lf)
            "text/csv; charset=utf-8")
  db)

(defn clipboard-button [button]
  (let [clipboard-atom (atom nil)
        get-url #(.-href js/location)]
    (r/create-class
     {:component-did-mount #(let [clipboard (new js/Clipboard (r/dom-node %) #js {:text get-url})]
                              (reset! clipboard-atom clipboard))
      :component-will-unmount #(when @clipboard-atom
                                 (.destroy @clipboard-atom)
                                 (reset! clipboard-atom nil))
      :reagent-render (fn [] button)})))

(defn- popup-content []
  [:div.ui.relaxed.middle.aligned.selection.list
   [:a.item {:on-click #(do (dispatch :viewer/export-as-xls) (close-popup))}
    [:i.file.excel.outline.icon]
    [:div.content (t :reports/export-as-xls)]]
   [clipboard-button
    [:a.item {:on-click #(do (notify (t :share/copied)) (close-popup))}
     [:i.copy.icon]
     [:div.content (t :share/copy-url)]]]
   [:a.item {:on-click #(do (dispatch :viewer/raw-data-requested) (close-popup))}
    [:i.align.justify.icon]
    [:div.content (t :raw-data/menu)]]])

(defn share-menu []
  [:a.icon.item {:on-click #(show-popup % popup-content {:position "bottom center"})
                 :title (t :share/title)}
   [:i.share.alternate.icon]])
