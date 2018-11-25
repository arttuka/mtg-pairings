(ns mtg-pairings-server.repl
  (:require [mount.core :as m]
            [ring.middleware.file-info :refer :all]
            [ring.middleware.file :refer :all]
            [figwheel-sidecar.repl-api :as figwheel]
            [clojure.tools.namespace.repl :as repl]
            [mtg-pairings-server.handler :refer [app]]
            [mtg-pairings-server.server :refer [run-server!]]
            [mtg-pairings-server.env :refer [env]]))

(defn get-handler []
  ;; #'app expands to (var app) so that when we reload our code,
  ;; the server is forced to re-resolve the symbol in the var
  ;; rather than having its own copy. When the root binding
  ;; changes, the server picks it up without having to restart.
  (-> #'app
      ; Makes static assets in $PROJECT_DIR/resources/public/ available.
      (wrap-file "resources")
      ; Content-Type, Content-Length, and Last Modified headers for files in body
      (wrap-file-info)))

(m/defstate figwheel
  :start (figwheel/start-figwheel!)
  :stop (figwheel/stop-figwheel!))

(m/defstate figwheel-autobuilder
  :start (figwheel/start-autobuild "app")
  :stop (figwheel/stop-autobuild "app"))

(m/defstate server
  :start (run-server! (get-handler) (:server-port env))
  :stop (server))

(defn cljs-repl []
  (figwheel/cljs-repl))

(defn restart []
  (m/stop)
  (repl/refresh :after `m/start))
