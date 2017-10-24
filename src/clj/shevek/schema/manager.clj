(ns shevek.schema.manager
  (:require [shevek.schema.metadata :refer [cubes cube-metadata time-boundary]]
            [shevek.schema.repository :refer [save-cube find-cubes]]
            [shevek.lib.collections :refer [detect]]
            [cuerdas.core :as str]
            [taoensso.timbre :refer [debug]]))

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
    (->> (concat old-updated-fields new-fields)
         (remove :hidden))))

(defn- set-default-title [{:keys [name title] :or {title (str/title name)} :as record}]
  (assoc record :title title))

(defn calculate-expression [{:keys [type name] :as measure}]
  (let [agg-fn (condp re-find (str type)
                 #"Max" "max"
                 #"Unique" "count-distinct"
                 "sum")]
    (str "(" agg-fn " $" name ")")))

(defn- set-default-expression [measure]
  (merge {:expression (calculate-expression measure)} measure))

(defn- set-default-type [dimension]
  (merge {:type "STRING"} dimension))

(defn set-default-titles [{:keys [dimensions measures] :as cube}]
  (-> (set-default-title cube)
      (assoc :dimensions (mapv (comp set-default-type set-default-title) dimensions))
      (assoc :measures (mapv (comp set-default-expression set-default-title) measures))))

(defn- update-cube [old new]
  (-> (merge old (dissoc new :dimensions :measures))
      (assoc :dimensions (merge-dimensions (:dimensions old) (:dimensions new)))
      (assoc :measures (merge-dimensions (:measures old) (:measures new)))
      set-default-titles))

(defn update-cubes [db new-cubes]
  (let [existing-cubes (find-cubes db)]
    (doseq [new-cube new-cubes]
      (save-cube db (update-cube (corresponding new-cube existing-cubes) new-cube)))))

(defn discover! [dw db]
  (debug "Discovering cubes...")
  (update-cubes db (discover-cubes dw))
  (debug "Done discovering cubes"))

(defn update-time-boundary! [dw db]
  (debug "Updating time boundary...")
  (doseq [{:keys [name] :as cube} (find-cubes db)]
    (->> (time-boundary dw name)
         (merge cube)
         (save-cube db)))
  (debug "Done updating time boundary"))

#_(discover! shevek.dw/dw shevek.db/db)
#_(update-time-boundary! shevek.dw/dw shevek.db/db)
