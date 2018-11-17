(ns mtg-pairings-server.main
  (:gen-class)
  (:require [mount.core :as m]
            [taoensso.timbre :as timbre]
            [taoensso.timbre.appenders.3rd-party.rolling :refer [rolling-appender]]
            [mtg-pairings-server.server :refer [run-server!]]
            [mtg-pairings-server.env :refer [env]]
            mtg-pairings-server.handler))

(def timbre-config {:level     :info
                    :appenders {:rolling (rolling-appender {:path "/var/log/pairings/pairings.log" :pattern :daily})
                                :println nil}})

(m/defstate server
  :start (do
           (timbre/merge-config! timbre-config)
           (run-server! mtg-pairings-server.handler/app (:server-port env)))
  :stop (server))

(defn -main []
  (timbre/info "Starting mtg-pairings...")
  (m/start))
