(ns mtg-pairings-server.repl
  (:require [mount.core :as m :refer [defstate]]
            [cheshire.generate :as json-gen]
            [figwheel.main.api :as figwheel]
            [taoensso.timbre :as timbre]
            [clojure.tools.namespace.repl :as repl]
            mtg-pairings-server.server
            mtg-pairings-server.db)
  (:import (com.fasterxml.jackson.core JsonGenerator)
           (org.joda.time LocalDate)))

(m/in-cljc-mode)
(timbre/swap-config! (fn [config] (assoc config :ns-blacklist ["org.eclipse.jetty.*" "io.netty.*" "com.zaxxer.hikari.*"])))
(json-gen/add-encoder LocalDate
                      (fn [c ^JsonGenerator generator]
                        (.writeString generator (str c))))

(def figwheel–build "dev")

(defstate figwheel
  :start (figwheel/start {:mode :serve} figwheel–build)
  :stop (figwheel/stop figwheel–build))

(defn cljs-repl []
  (figwheel/cljs-repl figwheel–build))

(defn restart []
  (m/stop)
  (repl/refresh :after `m/start))
