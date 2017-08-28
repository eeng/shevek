(ns shevek.schemas.user
  (:require [schema.core :as s]))

(s/defschema CubePermissions
  {:name s/Str})

(s/defschema Permissions
  (s/maybe {:allowed-cubes (s/cond-pre (s/eq "all") [CubePermissions])}))

(s/defschema User
  {(s/optional-key :_id) s/Any
   :username s/Str
   :fullname s/Str
   :password (s/constrained s/Str (comp pos? count))
   :admin s/Bool
   (s/optional-key :email) s/Str
   (s/optional-key :permissions) Permissions})

(defn admin? [user]
  (:admin user))
