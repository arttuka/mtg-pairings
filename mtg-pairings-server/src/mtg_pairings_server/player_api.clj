(ns mtg-pairings-server.player-api
  (:require [mtg-pairings-server.util :refer [response]]
            [compojure.core :as c]
            [mtg-pairings-server.players :refer :all]))

(defn routes []
  (c/routes
    (c/GET "/" []
      (response (players)))
    (c/GET "/:dci" [dci]
      (response (player dci)))
    (c/GET "/:dci/tournaments" [dci]
      (response (tournaments dci)))
    (c/GET "/:dci/latest" [dci]
      (response (latest dci)))))
