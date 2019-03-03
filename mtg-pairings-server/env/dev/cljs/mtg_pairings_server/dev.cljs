(ns ^:figwheel-no-load mtg-pairings-server.dev
  (:require [mount.core :as m]
            [mtg-pairings-server.core]))

(enable-console-print!)
(m/start)
