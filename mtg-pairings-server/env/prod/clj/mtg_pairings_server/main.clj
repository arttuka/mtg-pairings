(ns mtg-pairings-server.main
  (:gen-class)
  (:require [mount.core :as m]
            [mtg-pairings-server.server :refer [run-server!]]
            [mtg-pairings-server.properties :refer [properties]]))

(m/defstate server
  :start (run-server! #'mtg-pairings-server.handler/app (:server properties))
  :stop (server))

(defn -main []
  (m/start))
