(ns mtg-pairings-server.main
  (:gen-class)
  (:require [mount.core :as m]
            [taoensso.timbre :as timbre]
            [taoensso.timbre.appenders.3rd-party.rolling :refer [rolling-appender]]
            [mtg-pairings-server.server :refer [run-server!]]
            [mtg-pairings-server.properties :refer [properties]]))

(def timbre-config {:level :warn
                    :appenders {:rolling (rolling-appender :path "./logs/pairings.log" :pattern :daily)
                                :println nil}})

(m/defstate server
  :start (do
           (timbre/merge-config! timbre-config)
           (run-server! #'mtg-pairings-server.handler/app (:server properties)))
  :stop (server))

(defn -main []
  (m/start))
