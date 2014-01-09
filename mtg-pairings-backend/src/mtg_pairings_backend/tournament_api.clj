(ns mtg-pairings-backend.tournament-api
  (:require [ring.util.response :refer [response]]
            [compojure.core :as c]
            [mtg-pairings-backend.db :refer :all]))

(defn ^:private get-round [db id round]
  (let [tournament (tournament db (Integer/parseInt id))
        round-num (Integer/parseInt round)]
    (-> tournament :rounds round-num)))

(defn routes [db]
  (c/routes
    (c/POST "/" [:as request]
      (let [id (add-tournament db (:body request))]
        (response {:id id})))
    (c/GET "/:id" [id]
      (response (tournament db (Integer/parseInt id))))
    (c/GET "/:id/round-:round/pairings" [id round]
      (response (get-round db id round)))
    (c/GET "/:id/round-:round/results" [id round]
      (response (get-round db id round)))
    (c/PUT "/:id/round-:round/pairings" [id round :as request]
      (add-pairings db (Integer/parseInt id) (Integer/parseInt round) (:body request))
      {:status 200})
    (c/PUT "/:id/round-:round/results" [id round :as request]
      (add-results db (Integer/parseInt id) (Integer/parseInt round) (:body request))
      {:status 200})
    (c/PUT "/:id/teams" [id :as request]
      (add-teams db (Integer/parseInt id) (:body request)))
    (c/GET "/:id/standings" [id]
      (let [tournament (tournament db (Integer/parseInt id))]
        (response (:standings tournament))))))
