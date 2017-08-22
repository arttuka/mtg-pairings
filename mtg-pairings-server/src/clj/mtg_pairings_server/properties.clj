(ns mtg-pairings-server.properties
  (:require [clojure.edn :as edn]
            [mount.core :as m]))

(defn load-properties [path]
  (edn/read-string (slurp path)))

(m/defstate properties
  :start (load-properties "properties.edn"))
