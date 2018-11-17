(ns ^:figwheel-no-load mtg-pairings-server.dev
  (:require
    [mount.core :as m]
    [mtg-pairings-server.core]
    [devtools.core :as devtools]))

(devtools/install!)

(enable-console-print!)

(m/start)
