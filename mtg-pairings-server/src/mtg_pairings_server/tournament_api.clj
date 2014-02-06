(ns mtg-pairings-server.tournament-api
  (:require [ring.util.response :refer [response]]
            [compojure.core :as c]
            [mtg-pairings-server.tournaments :refer :all]))

(defn ^:private get-round [db id round]
  (let [tournament (tournament (Integer/parseInt id))
        round-num (Integer/parseInt round)]
    (get-in tournament [:rounds round-num])))

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
      (response (get-round id round)))
    (c/GET "/:id/round-:round/results" [id round]
      (response (get-round id round)))
    (c/PUT "/:id/round-:round/pairings" [id round :as request]
      (add-pairings (Integer/parseInt id) (Integer/parseInt round) (:body request))
      {:status 200})
    (c/PUT "/:id/round-:round/results" [id round :as request]
      (add-results (Integer/parseInt id) (Integer/parseInt round) (:body request))
      {:status 200})
    (c/PUT "/:id/teams" [id :as request]
      (add-teams (Integer/parseInt id) (:body request)))
    (c/GET "/:id/standings" [id]
      (let [tournament (tournament (Integer/parseInt id))]
        (response (:standings tournament))))))
