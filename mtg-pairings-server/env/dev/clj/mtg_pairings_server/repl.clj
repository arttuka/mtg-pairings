(ns mtg-pairings-server.repl
  (:require [mount.core :as m :refer [defstate]]
            [cheshire.generate :as json-gen]
            [figwheel.main.api :as figwheel]
            [taoensso.timbre :as timbre]
            [clojure.tools.namespace.repl :as repl]
            mtg-pairings-server.server)
  (:import (com.fasterxml.jackson.core JsonGenerator)
           (org.joda.time LocalDate)))

(m/in-cljc-mode)
(timbre/swap-config! (fn [config] (assoc config :ns-blacklist ["org.eclipse.jetty.*" "io.netty.*" "com.zaxxer.hikari.*"])))
(json-gen/add-encoder LocalDate
                      (fn [c ^JsonGenerator generator]
                        (.writeString generator (str c))))

(defstate figwheel
  :start (figwheel/start {:mode :serve} "dev")
  :stop (figwheel/stop "dev"))

(defn cljs-repl []
  (figwheel/cljs-repl "dev"))

(defn restart []
  (m/stop)
  (repl/refresh :after `m/start))
