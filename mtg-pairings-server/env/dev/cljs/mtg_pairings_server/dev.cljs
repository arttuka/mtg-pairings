(ns ^:figwheel-no-load mtg-pairings-server.dev
  (:require
    [mtg-pairings-server.core :as core]
    [devtools.core :as devtools]))

(devtools/install!)

(enable-console-print!)

(core/init!)
