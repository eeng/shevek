(ns shevek.pages.profile.helpers)

(defn avatar [{:keys [username]} & [{:keys [class]}]]
  [:img.ui.rounded.image.user-avatar
   {:src (str "https://api.adorable.io/avatars/50/" username)
    :class class}])
