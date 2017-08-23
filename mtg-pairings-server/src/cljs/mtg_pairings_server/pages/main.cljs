(ns mtg-pairings-server.pages.main
  (:require [mtg-pairings-server.components.tournament :refer [tournament-list]]))

(defn main-page []
  [tournament-list])
