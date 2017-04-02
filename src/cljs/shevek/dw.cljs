(ns shevek.dw
  (:require-macros [reflow.macros :refer [defevh]])
  (:require [reflow.core :refer [dispatch]]
            [cuerdas.core :as str]
            [shevek.rpc :as rpc]
            [shevek.lib.collections :refer [detect]]
            [shevek.lib.dates :as d :refer [parse-time to-iso8601]]
            [shevek.lib.collections :refer [detect]]
            [cljs-time.core :as t]
            [cljs-time.format :as f]
            [reflow.db :as db]
            [com.rpl.specter :refer [setval ALL]]))

(defn- set-default-title [{:keys [name title] :or {title (str/title name)} :as record}]
  (assoc record :title title))

(defn parse-max-time [max-time]
  (d/round-to-next-minute (parse-time max-time)))

(defn set-cube-defaults [{:keys [dimensions measures max-time] :as cube}]
  (cond-> (set-default-title cube)
          dimensions (assoc :dimensions (map set-default-title dimensions))
          measures (assoc :measures (map set-default-title measures))
          max-time (assoc :max-time (parse-max-time max-time))))

(defn- set-defaults [cubes]
  (map set-cube-defaults cubes))

(defn- to-map-with-name-as-key [cubes]
  (zipmap (map :name cubes) cubes))

(defevh :cubes-arrived [db cubes]
  (-> (update db :cubes #(merge-with merge % (to-map-with-name-as-key (set-defaults cubes))))
      (rpc/loaded :cubes)))

(defevh :cubes-requested [db]
  (rpc/call "dw/cubes" :handler #(dispatch :cubes-arrived %))
  (rpc/loading db :cubes))

(defn fetch-cubes []
  (dispatch :cubes-requested))

(defn cubes-list []
  (vals (db/get :cubes)))

(defn time-dimension? [{:keys [name]}]
  (= name "__time"))

; TODO esto fallaría si no hay una dimension __time
(defn time-dimension [dimensions]
  (some #(when (time-dimension? %) %) dimensions))

(defn dim=? [dim1 dim2]
  (= (:name dim1) (:name dim2)))

; TODO algunos de estos metodos no corresponderian en el shared?
(defn find-dimension [name dimensions]
  (detect #(= (:name %) name) dimensions))

(defn includes-dim? [coll dim]
  (some #(dim=? % dim) coll))

(defn add-dimension [coll dim]
  (let [coll (or coll [])]
    (if (includes-dim? coll dim)
      coll
      (conj coll dim))))

(defn remove-dimension [coll dim]
  (vec (remove #(dim=? dim %) coll)))

(defn replace-dimension [coll dim]
  (setval [ALL (partial dim=? dim)] dim coll))

(defn to-interval [selected-period max-time]
  (let [now (d/now)
        max-time (or max-time now)
        day-of-last-week (t/minus (d/beginning-of-week now) (t/days 1))
        day-of-last-month (t/minus (d/beginning-of-month now) (t/days 1))
        day-of-last-quarter (t/minus (d/beginning-of-quarter now) (t/days 1))
        day-of-last-year (t/minus (d/beginning-of-year now) (t/days 1))]
    (case selected-period
      :latest-hour [(t/minus max-time (t/hours 1)) max-time]
      :latest-6hours [(t/minus max-time (t/hours 6)) max-time]
      :latest-day [(t/minus max-time (t/days 1)) max-time]
      :latest-7days [(t/minus max-time (t/days 7)) max-time]
      :latest-30days [(t/minus max-time (t/days 30)) max-time]
      :current-day [(d/beginning-of-day now) (d/end-of-day now)]
      :current-week [(d/beginning-of-week now) (d/end-of-week now)]
      :current-month [(d/beginning-of-month now) (d/end-of-month now)]
      :current-quarter [(d/beginning-of-quarter now) (d/end-of-quarter now)]
      :current-year [(d/beginning-of-year now) (d/end-of-year now)]
      :previous-day [(d/beginning-of-day (d/yesterday)) (d/end-of-day (d/yesterday))]
      :previous-week [(d/beginning-of-week day-of-last-week) (d/end-of-week day-of-last-week)]
      :previous-month [(d/beginning-of-month day-of-last-month) (d/end-of-month day-of-last-month)]
      :previous-quarter [(d/beginning-of-quarter day-of-last-quarter) (d/end-of-quarter day-of-last-quarter)]
      :previous-year [(d/beginning-of-year day-of-last-year) (d/end-of-year day-of-last-year)])))

(defn format-period [period max-time]
  (let [formatter (d/formatter
                   (if (str/starts-with? (name period) "latest") :minute :day))]
    (->> (to-interval period max-time)
         (map #(f/unparse formatter %))
         distinct
         (str/join " - "))))

; Convierto manualmente los goog.dates en el intervalo a iso8601 strings porque sino explota transit xq no los reconoce. Alternativamente se podría hacer un handler de transit pero tendría que manejarme con dates en el server y por ahora usa los strings que devuelve Druid nomas.
(defn add-interval [{:keys [filter] :as q} max-time]
  (let [period (:selected-period (time-dimension filter))]
    (assoc q :interval (mapv to-iso8601 (to-interval period max-time)))))
