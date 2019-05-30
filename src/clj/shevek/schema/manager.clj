(ns shevek.schema.manager
  (:require [shevek.engine.protocol :refer [cubes cube-metadata time-boundary]]
            [shevek.engine.druid-native.metadata :refer [only-used-keys]]
            [shevek.schema.repository :refer [save-cube find-cubes]]
            [shevek.lib.collections :refer [detect]]
            [shevek.lib.logging :refer [benchmark]]
            [shevek.domain.dimension :refer [time-dimension]]
            [cuerdas.core :as str]))

(defn- discover-cubes [dw]
  (for [cube-name (cubes dw)]
    (merge {:name cube-name} (cube-metadata dw cube-name))))

(defn- same-name? [{n1 :name} {n2 :name}]
  (= n1 n2))

(defn- corresponding [field coll]
  (detect #(same-name? field %) coll))

(defn- update-dimension [old new]
  (if new
    (merge (only-used-keys old) new)
    old))

(defn- merge-dimensions [old-coll new-coll]
  (let [old-updated-fields (map #(update-dimension % (corresponding % new-coll)) old-coll)
        new-fields (remove #(corresponding % old-coll) new-coll)]
    (->> (concat old-updated-fields new-fields)
         (remove :hidden))))

(defn- set-default-title [{:keys [name title] :or {title (str/title name)} :as record}]
  (assoc record :title title))

(defn- set-default-type [dimension]
  (merge {:type "STRING"} dimension))

(defn set-defaults [{:keys [dimensions measures] :as cube}]
  (-> (set-default-title cube)
      (assoc :dimensions (mapv (comp set-default-type set-default-title) dimensions))
      (assoc :measures (mapv set-default-title measures))))

(defn- add-time-dimension [{:keys [dimensions] :as cube}]
  (cond-> cube
          (not (time-dimension dimensions)) (update :dimensions conj {:name "__time"})))

(defn- update-cube [old new]
  (-> (merge old (dissoc new :dimensions :measures))
      (assoc :dimensions (merge-dimensions (:dimensions old) (:dimensions new)))
      (assoc :measures (merge-dimensions (:measures old) (:measures new)))
      add-time-dimension
      set-defaults))

(defn update-cubes [db new-cubes]
  (let [existing-cubes (find-cubes db)]
    (doseq [new-cube new-cubes]
      (save-cube db (update-cube (corresponding new-cube existing-cubes) new-cube)))))

(defn discover! [dw db]
  (benchmark {:after "Cube discovering done (%.0f ms)"}
    (update-cubes db (discover-cubes dw))))

(defn update-time-boundary! [dw db]
  (benchmark {:after "Updated time boundary (%.0f ms)"}
    (doseq [{:keys [name] :as cube} (find-cubes db)]
      (->> (time-boundary dw name)
           (merge cube)
           (save-cube db)))))

#_(discover-cubes shevek.engine.state/dw)
#_(discover! shevek.engine.state/dw shevek.db/db)
#_(update-time-boundary! shevek.engine.state/dw shevek.db/db)
