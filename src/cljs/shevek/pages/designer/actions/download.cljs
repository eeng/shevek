(ns shevek.pages.designer.actions.download
  (:require [reagent.core :as r]
            [shevek.i18n :refer [t]]
            [shevek.components.popup :refer [tooltip]]
            [shevek.lib.time.ext :refer [format-time]]
            [shevek.lib.time :refer [now]]
            [shevek.domain.exporters.csv :as csv]
            [shevek.pages.designer.helpers :refer [build-visualization]]
            [cljsjs.filesaverjs]))

(defn download [filename content & [mime-type]]
  (let [mime-type (or mime-type (str "text/plain;charset=" (.-characterSet js/document)))
        blob (new js/Blob
                  (clj->js [content])
                  (clj->js {:type mime-type}))]
    (js/saveAs blob filename)))

(defn- download-csv [report report-results loading]
  (reset! loading true)
  (js/setTimeout ; So the loading indicator is shown immediately
   (fn []
     (download (str "report_" (format-time (now) :file))
               (csv/generate (build-visualization
                              report-results
                              report))
               "text/csv; charset=utf-8")
     (reset! loading false))
   0))

(defn download-csv-button [report report-results]
  (r/with-let [loading (r/atom false)]
    [:button.ui.default.icon.button
     {:ref (tooltip (t :reports/download-csv))
      :on-click #(download-csv report report-results loading)
      :class (when @loading "loading disabled")}
     [:i.download.icon]]))
