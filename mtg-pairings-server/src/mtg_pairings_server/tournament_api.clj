(ns mtg-pairings-server.tournament-api
  (:require [mtg-pairings-server.util :refer [response]]
            [compojure.core :as c]
            [mtg-pairings-server.tournaments :refer :all]
            [mtg-pairings-server.util :refer [parse-date]]))

(defmacro validate-request [sanction-id request & body]
  `(let [user# (user-for-request ~request)
         owner# (owner-of-tournament ~sanction-id)]
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
                           (assoc :owner user))]
          (add-tournament tournament)
          {:status 204})
        {:status 400}))
    (c/GET "/" []
      (response (tournaments)))
    (c/GET "/:id" [id]
      (response (tournament (Integer/parseInt id))))
    (c/GET "/:id/round-:round/pairings" [id round]
      (response (get-round (Integer/parseInt id) (Integer/parseInt round))))
    (c/GET "/:id/round-:round/results" [id round]
      (response (get-round (Integer/parseInt id) (Integer/parseInt round))))
    (c/GET "/:id/round-:round/standings" [id round :as request]
      (response (standings (Integer/parseInt id) (Integer/parseInt round) (get-in request [:params :secret]))))
    (c/GET "/:id/seatings" [id]
      (response (seatings (Integer/parseInt id))))
    (c/PUT "/:sanctionid/round-:round/pairings" [sanctionid round :as request]
      (validate-request sanctionid request
        (add-pairings sanctionid (Integer/parseInt round) (:body request))
        {:status 204}))
    (c/PUT "/:sanctionid/round-:round/results" [sanctionid round :as request]
      (validate-request sanctionid request
        (add-results sanctionid (Integer/parseInt round) (:body request))
        {:status 204}))
    (c/PUT "/:sanctionid/round-:round/results/publish" [sanctionid round :as request]
      (validate-request sanctionid request
        (publish-results sanctionid (Integer/parseInt round))
        {:status 204}))
    (c/PUT "/:sanctionid/seatings" [sanctionid :as request]
      (validate-request sanctionid request
        (add-seatings sanctionid (:body request))
        {:status 204}))
    (c/PUT "/:sanctionid/teams" [sanctionid :as request]
      (validate-request sanctionid request
        (add-teams sanctionid (:body request))
        {:status 204}))
    (c/DELETE "/:sanctionid" [sanctionid :as request]
      (validate-request sanctionid request
        (reset-tournament sanctionid)
        {:status 204}))))
