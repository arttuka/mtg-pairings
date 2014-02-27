(ns mtg-pairings-server.tournament-api
  (:require [mtg-pairings-server.util :refer [response]]
            [compojure.core :as c]
            [mtg-pairings-server.tournaments :refer :all]
            [mtg-pairings-server.util :refer [parse-date]]))

(defmacro validate-request [tournament-id request & body]
  `(let [user# (user-for-request ~request)
         owner# (owner-of-tournament (Integer/parseInt ~tournament-id))]
     (cond
       (nil? owner#) {:status 404}
       (nil? user#) {:status 400}
       (not= owner# user#) {:status 403}
       :else (do ~@body))))

(defn routes []
  (c/routes
    (c/POST "/" [:as request]
      (if-let [user (user-for-request request)]
        (let [tournament (-> (:body request)
                           (update-in [:day] parse-date)
                           (assoc :owner user))
              id (add-tournament tournament)]
          (response {:id id}))
        {:status 400}))
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
    (c/GET "/:id/seatings" [id]
      (response (seatings (Integer/parseInt id))))
    (c/PUT "/:id/round-:round/pairings" [id round :as request]
      (validate-request id request
        (add-pairings (Integer/parseInt id) (Integer/parseInt round) (:body request))
        {:status 200}))
    (c/PUT "/:id/round-:round/results" [id round :as request]
      (validate-request id request
        (add-results (Integer/parseInt id) (Integer/parseInt round) (:body request))
        {:status 200}))
    (c/PUT "/:id/seatings" [id :as request]
      (validate-request id request
        (add-seatings (Integer/parseInt id) (:body request))
        {:status 200}))
    (c/PUT "/:id/teams" [id :as request]
      (validate-request id request
        (add-teams (Integer/parseInt id) (:body request))
        {:status 200}))))
