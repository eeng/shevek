(ns shevek.schemas.user
  (:require [schema.core :as s]))

(s/defschema User
  {:username s/Str
   :fullname s/Str
   :password (s/constrained s/Str (comp pos? count))
   :admin s/Bool
   (s/optional-key :email) s/Str
   (s/optional-key :_id) s/Any})

(defn admin? [user]
  (:admin user))
