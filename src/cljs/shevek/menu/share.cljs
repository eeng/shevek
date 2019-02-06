; TODO DASHBOARD VUELA este y los demas en esta carpeta
(ns shevek.menu.share
  (:require [reagent.core :as r]
            [cljsjs.clipboard]
            [cljsjs.filesaverjs]
            [shevek.i18n :refer [t]]
            [shevek.components.popup :refer [show-popup close-popup]]
            [shevek.reflow.core :refer [dispatch] :refer-macros [defevh]]
            [shevek.components.notification :refer [notify]]
            [shevek.lib.time.ext :refer [format-time]]
            [shevek.lib.time :refer [now]]
            [shevek.domain.exporters.csv :as csv]
            [shevek.pages.designer.helpers :refer [build-visualization]]))

(defn download [filename content & [mime-type]]
  (let [mime-type (or mime-type (str "text/plain;charset=" (.-characterSet js/document)))
        blob (new js/Blob
                  (clj->js [content])
                  (clj->js {:type mime-type}))]
    (js/saveAs blob filename)))

(defevh :report/export-as-csv [db]
  (download (str "report_" (format-time (now) :file))
            (csv/generate (build-visualization
                           (get-in db [:designer :report-results])
                           (get-in db [:designer :report])))
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
   [:a.item {:on-click #(do (dispatch :report/export-as-csv) (close-popup))}
    [:i.file.excel.outline.icon]
    [:div.content (t :reports/export-as-csv)]]
   [clipboard-button
    [:a.item {:on-click #(do (notify (t :share/copied)) (close-popup))}
     [:i.copy.icon]
     [:div.content (t :share/copy-url)]]]
   [:a.item {:on-click #(do (dispatch :designer/raw-data-requested) (close-popup))}
    [:i.align.justify.icon]
    [:div.content (t :raw-data/menu)]]])

(defn share-menu []
  [:a.icon.item {:on-click #(show-popup % popup-content {:position "bottom center"})
                 :title (t :share/title)}
   [:i.share.alternate.icon]])
