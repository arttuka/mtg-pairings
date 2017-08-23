(ns ^:figwheel-no-load mtg-pairings-server.dev
  (:require
    [mount.core :as m]
    [mtg-pairings-server.core :as core]
    [devtools.core :as devtools]))

(devtools/install!)

(enable-console-print!)

(m/start)
