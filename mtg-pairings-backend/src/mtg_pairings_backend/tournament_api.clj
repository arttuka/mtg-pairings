(ns mtg-pairings-backend.tournament-api
  (:require [ring.util.response :refer [response]]
            [compojure.core :as c]
            [mtg-pairings-backend.db :refer :all]))

(defn routes [db]
  (c/routes
    (c/POST "/" [:as request]
      (let [id (add-tournament db (:body request))]
        (response {:id id})))
    (c/GET "/:id" [id]
      (response
        (tournament db (Integer/parseInt id))))))