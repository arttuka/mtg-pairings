(ns mtg-pairings-server.api
  (:require [compojure.api.sweet :refer :all]
            [compojure.api.swagger]
            [ring.util.http-response :refer :all]
            [mtg-pairings-server.util :refer [parse-date]]
            [mtg-pairings-server.tournaments :as tournament]
            [mtg-pairings-server.players :as player]
            [mtg-pairings-server.schema :refer :all]))

(defapi app
 (swagger-docs
   :title "WER pairings backend API")
 (swagger-ui)
 (swaggered "tournament"
   :description "Tournament API"
   (context "/api" []
     (POST* "/tournament" []
       :return Tournament
       :summary "Adds a tournament"
       :nickname "addTournament"
       :body [tournament InputTournament {:description "new tournament"}]
       (ok (tournament/add-tournament (update-in tournament [:day] parse-date))))
     (GET* "/tournament" []
       :return [Tournament]
       :summary "Gets all tournaments"
       :nickname "getTournaments"
       (ok (tournament/tournaments)))
     (GET* "/tournament/:tournament-id" [tournament-id]
       :return Tournament
       :summary "Gets a tournament by id"
       :nickname "getTournament"
       (ok (tournament/tournament (->Long tournament-id))))
     (GET* "/tournament/:tournament-id/round-:round/pairings" [tournament-id round]
       :return [Pairing]
       :summary "Gets pairings of a round in a tournament"
       :nickname "getRound"
       (ok (tournament/get-round (->Long tournament-id) (->Long round))))
     (GET* "/tournament/:tournament-id/round-:round/results" [tournament-id round]
       :return [Pairing]
       :summary "Gets results of a round in a tournament"
       :nickname "getResults"
       (ok (tournament/get-round (->Long tournament-id) (->Long round))))
     (GET* "/tournament/:tournament-id/round-:round/standings" [tournament-id round]
       :return [Standing]
       :summary "Gets standings after a round in a tournament"
       :nickname "getStandings"
       (ok (tournament/standings (->Long tournament-id) (->Long round))))
     (GET* "/tournament/:tournament-id/seatings" [tournament-id]
       :return [Seating]
       :summary "Gets seatings of a tournament"
       :nickname "getSeatings"
       (ok (tournament/seatings (->Long tournament-id))))
     (POST* "/tournament/:tournament-id/round-:round/pairings" [tournament-id round]
       :body [pairings InputPairings {:description "pairings"}]
       :summary "Adds pairings for round"
       :nickname "addPairings"
       (ok (tournament/add-pairings (->Long tournament-id) (->Long round) pairings)))
     (PUT* "/tournament/:tournament-id/round-:round/results" [tournament-id round]
       :body [results InputResults {:description "results"}]
       :summary "Adds results for round"
       :nickname "addResults"
       (tournament/add-results (->Long tournament-id) (->Long round) results)
       (ok))
     (PUT* "/tournament/:tournament-id/seatings" [tournament-id]
       :body [seatings InputSeatings {:description "seatings"}]
       :summary "Adds seatings for tournament"
       :nickname "addSeatings"
       (tournament/add-seatings (->Long tournament-id) seatings)
       (ok))
     (PUT* "/tournament/:tournament-id/teams" [tournament-id]
       :body [teams InputTeams {:description "teams"}]
       :summary "Adds teams for tournament"
       :nickname "addTeams"
       (tournament/add-teams (->Long tournament-id) teams)
       (ok))
     (GET* "/player" []
       :return [Player]
       :summary "Gets all players"
       :nickname "getPlayers"
       (ok (player/players)))
     (GET* "/player/:dci" [dci]
       :return Player
       :summary "Gets a player by DCI number"
       :nickname "getPlayer"
       (ok (player/player dci)))
     (GET* "/player/:dci/tournaments" [dci]
       :return [PlayersTournament]
       :summary "Gets all tournaments of a players"
       :nickname "getPlayersTournaments"
       (ok (player/tournaments dci))))))
