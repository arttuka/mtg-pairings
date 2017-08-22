(ns mtg-pairings-server.http-api
  (:require [compojure.api.sweet :refer :all]
            [ring.util.http-response :refer :all]
            mtg-pairings-server.tournament-api
            mtg-pairings-server.player-api
            [mtg-pairings-server.properties :refer [properties]]
            [mtg-pairings-server.util :refer [edn-response]]
            [mtg-pairings-server.schema :refer :all]))

(defapi app
  (swagger-routes
    {:ui "/api-docs"
     :spec "/swagger.json"
     :data {:info {:title "WER pairings backend API"}}})
  (undocumented
    (GET "/version" [] (edn-response (get-in properties [:server :version]))))
  (context "/api" []
    (context "/tournament" [] mtg-pairings-server.tournament-api/tournament-routes)
    (context "/player" [] mtg-pairings-server.player-api/player-routes)))
