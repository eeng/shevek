(ns shevek.schema.manager
  (:require [shevek.engine.protocol :refer [cubes cube-metadata time-boundary]]
            [shevek.schema.repository :refer [save-cube find-cubes]]
            [shevek.lib.collections :refer [detect]]
            [shevek.lib.logging :refer [benchmark]]
            [shevek.config :refer [config]]
            [cuerdas.core :as str]))

(defn- discover-cubes [dw]
  (for [cube-name (cubes dw)]
    (merge {:name cube-name} (cube-metadata dw cube-name))))

(defn- same-name? [{n1 :name} {n2 :name}]
  (= n1 n2))

(defn- corresponding [field coll]
  (detect #(same-name? field %) coll))

(defn- merge-dimensions [old-coll new-coll]
  (let [old-updated-fields (map #(merge % (corresponding % new-coll)) old-coll)
        new-fields (remove #(corresponding % old-coll) new-coll)]
    (concat old-updated-fields new-fields)))

(defn- set-default-title [{:keys [name title] :or {title (str/title name)} :as record}]
  (assoc record :title title))

(defn- set-default-type [dimension]
  (merge {:type "STRING"} dimension))

(defn set-defaults [cube]
  (-> (set-default-title cube)
      (update :dimensions #(mapv (comp set-default-type set-default-title) %))
      (update :measures #(mapv set-default-title %))))

(defn- remove-hidden-dimensions [cube]
  (-> cube
      (update :dimensions #(remove :hidden %))
      (update :measures #(remove :hidden %))))

(defn- merge-cube [{discovered-dimensions :dimensions discovered-measures :measures :as discovered-cube}
                   {configured-dimensions :dimensions configured-measures :measures :as configured-cube}]
  (let [configured-measure? #(corresponding % configured-measures)
        discovered-measures (concat discovered-measures (filter configured-measure? discovered-dimensions))
        discovered-dimensions (remove configured-measure? discovered-dimensions)]
    (-> (merge discovered-cube (dissoc configured-cube :dimensions :measures))
        (assoc :dimensions (merge-dimensions discovered-dimensions configured-dimensions))
        (assoc :measures (merge-dimensions discovered-measures configured-measures)))))

(defn merge-cubes [discovered-cubes configured-cubes]
  (let [discovered-configured (map #(merge-cube % (corresponding % configured-cubes)) discovered-cubes)
        non-discovered (remove #(corresponding % discovered-cubes) configured-cubes)]
    (->> (concat discovered-configured non-discovered)
         (map remove-hidden-dimensions)
         (map set-defaults))))

(defn update-cubes! [db new-cubes]
  (let [existing-cubes (find-cubes db)]
    (doseq [new-cube new-cubes :let [existing-cube (corresponding new-cube existing-cubes)]]
      (save-cube db (merge existing-cube new-cube)))))

(defn seed-schema!
  "Retrieves the cubes config, optionally performing auto-discovery of the schema, and saves it to the database (updating it if already exists)."
  [db dw {:keys [discover?] :or {discover? false}}]
  (benchmark {:after "Schema seeding done (%.0f ms)"}
    (let [discovered (if discover? (discover-cubes dw) [])
          configured (config :cubes)]
      (->> (merge-cubes discovered configured)
           (update-cubes! db)))))

; Time boundary

(defn update-time-boundary! [dw db]
  (benchmark {:after "Time boundary updated (%.0f ms)"}
    (doseq [{:keys [name] :as cube} (find-cubes db)]
      (->> (time-boundary dw name)
           (merge cube)
           (save-cube db)))))

; Examples

#_(find-cubes shevek.db/db)
#_(->>
   (merge-cubes
    (discover-cubes shevek.engine.state/dw)
    (config :cubes))
   (detect #(= (:name %) "wikipedia")))
#_(seed-schema! shevek.db/db shevek.engine.state/dw {:discover? true})
#_(discover! shevek.engine.state/dw shevek.db/db)
#_(update-time-boundary! shevek.engine.state/dw shevek.db/db)
