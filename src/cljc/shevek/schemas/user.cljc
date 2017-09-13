(ns shevek.schemas.user
  (:require [schema.core :as s]))

(s/defschema CubePermissions
  {:name s/Str
   (s/optional-key :measures) (s/cond-pre (s/eq "all") [s/Str])})

(s/defschema User
  {(s/optional-key :id) s/Str
   :username s/Str
   :fullname s/Str
   :password (s/constrained s/Str (comp pos? count))
   :admin s/Bool
   (s/optional-key :email) s/Str
   (s/optional-key :allowed-cubes) (s/cond-pre (s/eq "all") [CubePermissions])
   (s/optional-key :created-at) s/Any
   (s/optional-key :updated-at) s/Any})

(defn admin? [user]
  (:admin user))
