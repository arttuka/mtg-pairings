(ns mtg-pairings-server.player-api
  (:require [compojure.api.sweet :refer :all]
            [schema.core :as s]
            [mtg-pairings-server.util :refer [response]]
            [mtg-pairings-server.players :refer :all]
            [mtg-pairings-server.schema :refer :all]))

(defroutes player-routes
  (GET "/" []
    :return [Player]
    :summary "Hae kaikki pelaajat"
    (response (players)))
  (GET "/:dci" []
    :path-params [dci :- s/Str]
    :return Player
    :summary "Hae pelaaja DCI-numeron perusteella"
    (response (player dci)))
  (GET "/:dci/tournaments" []
    :path-params [dci :- s/Str]
    :summary "Hae tietyn pelaajan turnaukset"
    (response (tournaments dci))))