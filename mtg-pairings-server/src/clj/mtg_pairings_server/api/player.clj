(ns mtg-pairings-server.api.player
  (:require [compojure.api.sweet :refer :all]
            [schema.core :as s]
            [mtg-pairings-server.service.player :refer :all]
            [mtg-pairings-server.util.schema :refer :all]
            [mtg-pairings-server.util :refer [response]]))

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
