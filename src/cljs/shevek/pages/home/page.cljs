(ns shevek.pages.home.page
  (:require [shevek.i18n :refer [t]]
            [shevek.components.layout :refer [page-with-header]]))

(defn page []
  [page-with-header
   {:title (t :home/title) :subtitle (t :home/subtitle) :icon "home"}])
