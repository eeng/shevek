(ns pivot.settings
  (:require [pivot.i18n :refer [t]]))

(defn page []
  [:div
   [:h1.ui.dividing.header (t :menu/settings)]])
