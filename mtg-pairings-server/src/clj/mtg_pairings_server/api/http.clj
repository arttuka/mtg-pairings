(ns mtg-pairings-server.api.http
  (:require [compojure.api.sweet :refer :all]
            [mtg-pairings-server.api.player :refer [player-routes]]
            [mtg-pairings-server.api.tournament :refer [tournament-routes]]))

(defapi app
  (swagger-routes
   {:ui   "/api-docs"
    :spec "/swagger.json"
    :data {:info {:title "WER pairings backend API"}}})
  (context "/api" []
    (context "/tournament" [] tournament-routes)
    (context "/player" [] player-routes)))
