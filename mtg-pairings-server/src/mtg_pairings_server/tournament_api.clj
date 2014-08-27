(ns mtg-pairings-server.tournament-api
  (:require [mtg-pairings-server.util :refer [response]]
            [mtg-pairings-server.tournaments :refer :all]
            [mtg-pairings-server.util :refer [parse-date]]
            [mtg-pairings-server.schema :refer :all]
            clj-time.coerce
            [compojure.api.sweet :refer :all]
            compojure.api.swagger))

(defmacro validate-request [sanction-id apikey & body]
  `(let [user# (user-for-apikey ~apikey)
         owner# (owner-of-tournament ~sanction-id)]
     (cond
       (nil? owner#) {:status 404}
       (nil? user#) {:status 400}
       (not= owner# user#) {:status 403}
       :else (do ~@body))))

(defroutes* tournament-routes
  (POST* "/" request
    :return Tournament
    :summary "Lisää turnaus"
    :query-params [key :- String]
    :body [tournament InputTournament {:description "Uusi turnaus"}]
    (if-let [user (user-for-apikey key)]
      (let [tournament (-> tournament
                         (update-in [:day] clj-time.coerce/to-local-date)
                         (assoc :owner user))]
        (add-tournament tournament)
        {:status 204})
      {:status 400}))
  (GET* "/" []
    :return [Tournament]
    :summary "Hae kaikki turnaukset"
    (response (tournaments)))
  (GET* "/:id" [id]
    :return Tournament
    :summary "Hae turnaus ID:n perusteella"
    (response (tournament (Integer/parseInt id))))
  (GET* "/:id/round-:round/pairings" [id round]
    :return [Pairing]
    :summary "Hae yhden kierroksen pairingit"
    (response (get-round (Integer/parseInt id) (Integer/parseInt round))))
  (GET* "/:id/round-:round/results" [id round]
    :return [Pairing]
    :summary "Hae yhden kierroksen tulokset"
    (response (get-round (Integer/parseInt id) (Integer/parseInt round))))
  (GET* "/:id/round-:round/standings" [id round :as request]
    :return [Standing]
    :summary "Hae kierroksen jälkeiset standingsit"
    (response (standings-for-api (Integer/parseInt id) (Integer/parseInt round) (get-in request [:params :secret]))))
  (GET* "/:id/seatings" [id]
    :return [Seating]
    :summary "Hae seatingit"
    (response (seatings (Integer/parseInt id))))
  (PUT* "/:sanctionid/round-:round/pairings" [sanctionid round]
    :body [pairings InputPairings {:description "pairings"}]
    :summary "Lisää kierroksen pairingit"
    :query-params [key :- String]
    (validate-request sanctionid key
      (add-pairings sanctionid (Integer/parseInt round) (:pairings pairings))
      {:status 204}))
  (PUT* "/:sanctionid/round-:round/results" [sanctionid round]
    :body [results InputResults {:description "results"}]
    :summary "Lisää kierroksen tulokset"
    :query-params [key :- String]
    (validate-request sanctionid key
      (add-results sanctionid (Integer/parseInt round) (:results results))
      {:status 204}))
  (PUT* "/:sanctionid/round-:round/results/publish" [sanctionid round]
    :summary "Julkaisee kierroksen tulokset"
    :query-params [key :- String]
    (validate-request sanctionid key
      (publish-results sanctionid (Integer/parseInt round))
      {:status 204}))
  (PUT* "/:sanctionid/seatings" [sanctionid]
    :body [seatings InputSeatings {:description "seatings"}]
    :summary "Lisää turnauksen seatingit"
    :query-params [key :- String]
    (validate-request sanctionid key
      (add-seatings sanctionid (:seatings seatings))
      {:status 204}))
  (PUT* "/:sanctionid/teams" [sanctionid]
    :body [teams InputTeams {:description "teams"}]
    :summary "Lisää turnauksen tiimit"
    :query-params [key :- String]
    (validate-request sanctionid key
      (add-teams sanctionid (:teams teams))
      {:status 204}))
  (DELETE* "/:sanctionid" [sanctionid]
    :summary "Poistaa kaikki turnauksen tiedot"
    :query-params [key :- String]
    (validate-request sanctionid key
      (reset-tournament sanctionid)
      {:status 204})))
