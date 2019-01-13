(ns mtg-pairings-server.repl
  (:require [mount.core :as m]
            [figwheel.main.api :as figwheel]
            [taoensso.timbre :as timbre]
            [clojure.tools.namespace.repl :as repl]
            mtg-pairings-server.server))

(m/in-cljc-mode)
(timbre/swap-config! (fn [config] (assoc config :ns-whitelist ["mtg-pairings-server.*"])))

(m/defstate figwheel
  :start (figwheel/start {:mode :serve} "dev")
  :stop (figwheel/stop "dev"))

(defn cljs-repl []
  (figwheel/cljs-repl "dev"))

(defn restart []
  (m/stop)
  (repl/refresh :after `m/start))
