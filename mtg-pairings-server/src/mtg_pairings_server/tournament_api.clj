(ns mtg-pairings-server.tournament-api
  (:require [mtg-pairings-server.util :refer [response]]
            [compojure.core :as c]
            [mtg-pairings-server.tournaments :refer :all]))

(defn routes []
  (c/routes
    (c/POST "/" [:as request]
      (let [id (add-tournament (:body request))]
        (response {:id id})))
    (c/GET "/" []
      (response (tournaments)))
    (c/GET "/:id" [id]
      (response (tournament (Integer/parseInt id))))
    (c/GET "/:id/round-:round/pairings" [id round]
      (response (get-round (Integer/parseInt id) (Integer/parseInt round))))
    (c/GET "/:id/round-:round/results" [id round]
      (response (get-round (Integer/parseInt id) (Integer/parseInt round))))
    (c/GET "/:id/round-:round/standings" [id round]
      (response (standings (Integer/parseInt id) (Integer/parseInt round))))
    (c/PUT "/:id/round-:round/pairings" [id round :as request]
      (add-pairings (Integer/parseInt id) (Integer/parseInt round) (:body request))
      {:status 200})
    (c/PUT "/:id/round-:round/results" [id round :as request]
      (add-results (Integer/parseInt id) (Integer/parseInt round) (:body request))
      {:status 200})
    (c/PUT "/:id/teams" [id :as request]
      (add-teams (Integer/parseInt id) (:body request)))
    ))
