(ns mtg-pairings-server.api.http
  (:require [compojure.api.sweet :refer :all]
            mtg-pairings-server.api.tournament
            mtg-pairings-server.api.player))

(defapi app
  (swagger-routes
   {:ui   "/api-docs"
    :spec "/swagger.json"
    :data {:info {:title "WER pairings backend API"}}})
  (context "/api" []
    (context "/tournament" [] mtg-pairings-server.api.tournament/tournament-routes)
    (context "/player" [] mtg-pairings-server.api.player/player-routes)))
