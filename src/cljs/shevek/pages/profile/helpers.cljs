(ns shevek.pages.profile.helpers
  (:require [reagent.core :as r]))

(defn avatar [{:keys [username]} & [{:keys [class]}]]
  (let [image-available (r/atom true)]
    (fn []
      (if @image-available
        [:img.ui.rounded.image.user-avatar
         {:src (str "https://api.adorable.io/avatars/50/" username)
          :class class
          :onError #(reset! image-available false)}]
        [:i.user.icon]))))
