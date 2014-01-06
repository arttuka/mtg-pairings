(ns mtg-pairings-backend.server
  (:gen-class)
  (:require [org.httpkit.server :as hs]
            [compojure.core :as c]))

(c/defroutes routes
  (c/GET "/" []
    "Hello world"))

(defn run! []
  (hs/run-server routes {:port 8080}))

(defn -main []
  (run!))