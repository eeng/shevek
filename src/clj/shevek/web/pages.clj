(ns shevek.web.pages
  (:require [hiccup.page :refer [html5 include-js]]
            [optimus.html :refer [link-to-css-bundles link-to-js-bundles]]))

(defn index [request]
  (html5
   [:head
    [:meta {:charset "utf-8"}]
    [:meta {:name "viewport" :content "width=device-width, initial-scale=1.0"}]
    [:title "Shevek"]
    (link-to-css-bundles request ["libs.css"])
    (link-to-css-bundles request ["app.css"])
    [:body
     [:div.ui.active.large.loader.preloader]
     [:div#app]
     (link-to-js-bundles request ["libs.js"])
     (link-to-js-bundles request ["app.js"])]]))
