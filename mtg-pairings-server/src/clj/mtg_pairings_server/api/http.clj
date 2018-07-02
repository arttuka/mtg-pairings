(ns mtg-pairings-server.api.http
  (:require [compojure.api.sweet :refer :all]
            [ring.util.http-response :refer :all]
            mtg-pairings-server.api.tournament
            mtg-pairings-server.api.player
            [mtg-pairings-server.properties :refer [properties]]
            [mtg-pairings-server.util.schema :refer :all]
            [mtg-pairings-server.util.util :refer [edn-response]]))

(defapi app
  (swagger-routes
    {:ui   "/api-docs"
     :spec "/swagger.json"
     :data {:info {:title "WER pairings backend API"}}})
  (undocumented
    (GET "/version" [] (edn-response (get-in properties [:server :version]))))
  (context "/api" []
    (context "/tournament" [] mtg-pairings-server.api.tournament/tournament-routes)
    (context "/player" [] mtg-pairings-server.api.player/player-routes)))
