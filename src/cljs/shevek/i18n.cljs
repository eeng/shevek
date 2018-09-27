(ns shevek.i18n
  (:require [tongue.core :as tongue]
            [shevek.reflow.db :as db]
            [shevek.locales.en :as en]
            [shevek.locales.es :as es]))

(def translations
  {:en en/translations
   :es es/translations
   :tongue/fallback :en})

(def translate (tongue/build-translate translations))

(defn lang []
  (keyword (db/get-in [:settings :lang] "en")))

(defn t [& args]
  (apply translate (lang) args))

(defn translation [& args]
  (or (get-in translations (into [(lang)] args))
      (when-not (= (lang) :en)
        (get-in translations (into [:en] args)))))

(defn translation! [& args]
  (or (apply translation args)
      (apply str "{Missing key " args "}")))
