(ns mtg-pairings-server.repl
  (:use mtg-pairings-server.handler
        figwheel-sidecar.repl-api
        ring.server.standalone
        [ring.middleware file-info file])
  (:require [mount.core :as m]
            [mtg-pairings-server.server :refer [run-server!]]
            [mtg-pairings-server.properties :refer [properties]]))

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

(m/defstate server
  :start (run-server! (get-handler) (:server properties))
  :stop (.stop server))
