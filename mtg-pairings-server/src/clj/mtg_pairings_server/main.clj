(ns mtg-pairings-server.main
  (:gen-class)
  (:require [mount.core :as m]
            [taoensso.timbre :as timbre]
            [taoensso.timbre.appenders.3rd-party.rolling :refer [rolling-appender]]
            mtg-pairings-server.server))

(def timbre-config {:level     :info
                    :appenders {:rolling (rolling-appender {:path "/var/log/pairings/pairings.log" :pattern :daily})}})

(defn -main []
  (timbre/merge-config! timbre-config)
  (timbre/info "Starting mtg-pairings...")
  (m/in-cljc-mode)
  (m/start))
