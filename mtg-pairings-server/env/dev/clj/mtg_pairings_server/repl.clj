(ns mtg-pairings-server.repl
  (:require [mount.core :as m]
            [figwheel-sidecar.repl-api :as figwheel]
            [clojure.tools.namespace.repl :as repl]
            mtg-pairings-server.server))

(m/defstate figwheel
  :start (figwheel/start-figwheel!)
  :stop (figwheel/stop-figwheel!))

(m/defstate figwheel-autobuilder
  :start (figwheel/start-autobuild "app")
  :stop (figwheel/stop-autobuild "app"))

(defn cljs-repl []
  (figwheel/cljs-repl))

(defn restart []
  (m/stop)
  (repl/refresh :after `m/start))
