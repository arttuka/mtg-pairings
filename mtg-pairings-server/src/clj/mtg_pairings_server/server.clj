(ns mtg-pairings-server.server
  (:require [taoensso.timbre :as log]
            [mount.core :refer [defstate]]
            [org.httpkit.server :as httpkit]
            [mtg-pairings-server.handler]
            [config.core :refer [env]]))

(defstate server
  :start (let [port (env :server-port)]
           (log/info "Starting server on port" port "...")
           (httpkit/run-server #'mtg-pairings-server.handler/app {:port port}))
  :stop (@server))
