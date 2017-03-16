(ns pivot.cube-view.shared
  (:require-macros [reflow.macros :refer [defevh]])
  (:require [reflow.core :refer [dispatch]]
            [reflow.db :as db]
            [reflow.db :as db]
            [pivot.lib.react :refer [with-react-keys]]
            [pivot.rpc :as rpc]
            [pivot.dw :as dw]
            [goog.string :as str]))

; Shared helpers

(defn- cube-view [& keys]
  (db/get-in (into [:cube-view] keys)))

(defn current-cube-name []
  (cube-view :cube))

(defn current-cube [cube-key]
  (-> (db/get :cubes)
      (get (current-cube-name))
      cube-key))

(defevh :query-executed [db results results-keys]
  (-> (assoc-in db (into [:cube-view] results-keys) results)
      (rpc/loaded results-keys)))

(defn send-query [db q results-keys]
  (rpc/call "dw/query"
            :args [(dw/to-dw-query q)]
            :handler #(dispatch :query-executed % results-keys))
  (rpc/loading db results-keys))

(defn- send-main-query [{:keys [cube-view] :as db}]
  (send-query db cube-view [:results :main]))

(defn includes-dim? [coll dim]
  (some #(dw/dim=? % dim) coll))

(defn add-dimension [coll dim]
  (let [coll (or coll [])]
    (if (includes-dim? coll dim)
      coll
      (conj coll dim))))

(defn remove-dimension [coll dim]
  (vec (remove #(dw/dim=? dim %) coll)))

(defn format-measure [value {:keys [type]}]
  (condp = type
    "doubleSum" (str/format "%.2f" value)
    "hyperUnique" (str/format "%d" value)
    value))

; Shared components

(defn- panel-header [text & actions]
  [:h2.ui.sub.header text
   (when (seq actions) [:div.actions (with-react-keys actions)])])
