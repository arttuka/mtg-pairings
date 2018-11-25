(ns mtg-pairings-server.env
  (:require [mount.core :as m]
            [config.core :refer [load-env]]))

(m/defstate env
  :start (load-env))
