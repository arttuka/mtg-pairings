(ns mtg-pairings-server.repl
  (:require [mount.core :as m :refer [defstate]]
            [cheshire.generate :as json-gen]
            [taoensso.timbre :as timbre]
            [clojure.tools.namespace.repl :as repl]
            [shadow.cljs.devtools.api :as cljs]
            [shadow.cljs.devtools.server :as shadow]
            mtg-pairings-server.server
            mtg-pairings-server.db)
  (:import (com.fasterxml.jackson.core JsonGenerator)
           (org.joda.time LocalDate)))

(m/in-cljc-mode)
(timbre/swap-config! (fn [config] (assoc config :ns-blacklist ["org.eclipse.jetty.*" "io.netty.*" "com.zaxxer.hikari.*"])))
(json-gen/add-encoder LocalDate
                      (fn [c ^JsonGenerator generator]
                        (.writeString generator (str c))))

(defstate shadow-cljs
  :start (do
           (shadow/start!)
           (cljs/watch :dev))
  :stop (do
          (shadow/stop!)
          (cljs/stop-worker :dev)))

(defn cljs-repl []
  (cljs/repl :dev))

(defn restart []
  (m/stop)
  (repl/refresh :after `m/start))
