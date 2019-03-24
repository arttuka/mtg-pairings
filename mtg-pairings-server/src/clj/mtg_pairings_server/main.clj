(ns mtg-pairings-server.main
  (:gen-class)
  (:require [mount.core :as m]
            [cheshire.generate :as json-gen]
            [taoensso.timbre :as timbre]
            [taoensso.timbre.appenders.3rd-party.rolling :refer [rolling-appender]]
            mtg-pairings-server.server)
  (:import (com.fasterxml.jackson.core JsonGenerator)
           (org.joda.time LocalDate)))

(def timbre-config {:level     :info
                    :appenders {:rolling (rolling-appender {:path "/var/log/pairings/pairings.log" :pattern :daily})}})

(defn -main []
  (json-gen/add-encoder LocalDate
                        (fn [c ^JsonGenerator generator]
                          (.writeString generator (str c))))
  (timbre/merge-config! timbre-config)
  (timbre/info "Starting mtg-pairings...")
  (m/in-cljc-mode)
  (try
    (m/start)
    (catch Throwable t
      (timbre/error t "Error while starting server")
      (m/stop)
      (System/exit 1))))
