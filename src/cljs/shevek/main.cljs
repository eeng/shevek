(ns shevek.main
  (:require [reagent.core :as r]))

(defn page []
  [:div.ui.grid
   [:div.two.wide.column
    "Dimensiones"]
   [:div.twelve.wide.column
    "Filtros"]
   [:div.two.wide.column
    "Pinned"]])
