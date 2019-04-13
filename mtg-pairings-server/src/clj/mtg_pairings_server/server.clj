(ns mtg-pairings-server.server
  (:require [taoensso.timbre :as log]
            [mount.core :refer [defstate]]
            [aleph.http :as http]
            [mtg-pairings-server.events]
            [mtg-pairings-server.handler]
            [config.core :refer [env]]))

(defstate ^{:on-reload :noop} server
  :start (let [port (env :server-port)]
           (log/info "Starting server on port" port "...")
           (http/start-server #'mtg-pairings-server.handler/app {:port port}))
  :stop (.close @server))
