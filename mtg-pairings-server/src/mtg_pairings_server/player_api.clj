(ns mtg-pairings-server.player-api
  (:require [mtg-pairings-server.util :refer [response]]
            [mtg-pairings-server.players :refer :all]
            [mtg-pairings-server.schema :refer :all]
            [compojure.api.sweet :refer :all]
            compojure.api.swagger))

(defroutes* player-routes
  (GET* "/" []
    :return [Player]
    :summary "Hae kaikki pelaajat"
    (response (players)))
  (GET* "/:dci" [dci]
    :return Player
    :summary "Hae pelaaja DCI-numeron perusteella"
    (response (player dci)))
  (GET* "/:dci/tournaments" [dci]
    :summary "Hae tietyn pelaajan turnaukset"
    (response (tournaments dci))))