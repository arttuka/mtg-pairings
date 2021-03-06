(ns mtg-pairings-server.main
  (:gen-class)
  (:require [mount.core :as m]
            [cheshire.generate :as json-gen]
            [config.core :refer [env]]
            [taoensso.timbre :as timbre]
            [mtg-pairings-server.migrations :as migrations]
            mtg-pairings-server.server
            mtg-pairings-server.db)
  (:import (com.fasterxml.jackson.core JsonGenerator)
           (org.joda.time LocalDate)))

(defn -main []
  (json-gen/add-encoder LocalDate
                        (fn [c ^JsonGenerator generator]
                          (.writeString generator (str c))))
  (timbre/merge-config! {:min-level :info})
  (when-not (env :dev)
    (timbre/info "Applying migrations...")
    (migrations/migrate))
  (timbre/info "Starting mtg-pairings...")
  (m/in-cljc-mode)
  (try
    (m/start)
    (catch Throwable t
      (timbre/error t "Error while starting server")
      (m/stop)
      (System/exit 1))))
