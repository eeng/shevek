(ns shevek.pages.profile.helpers)

(defn avatar [{:keys [id]} & [{:keys [class]}]]
  [:img.ui.rounded.image.user-avatar
   {:src (str "https://api.adorable.io/avatars/50/" id)
    :class class}])
