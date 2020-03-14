(ns mtg-pairings-server.api.decklist
  (:require [compojure.api.sweet :refer :all]
            [schema.core :as s]
            [clj-time.coerce :as tc]
            [mtg-pairings-server.db :as db]
            [mtg-pairings-server.service.decklist :refer :all]
            [mtg-pairings-server.service.tournament :refer [user-for-apikey]]
            [mtg-pairings-server.util :refer [response]]
            [mtg-pairings-server.util.schema :refer :all]))

(defroutes decklist-routes
  (POST "/" []
    :query-params [key :- s/Str]
    :body [tournament InputDecklistTournament]
    :return DecklistTournament
    (db/with-transaction
     (let [user (user-for-apikey key)
           existing (some-> (:id tournament) (get-organizer-tournament))]
       (cond
         (nil? user) {:status 400
                      :body   "Virheellinen API key"}
         (and existing (not= (:user existing) user)) {:status 403
                                                      :body   "Eri käyttäjän tallentama turnaus"}
         :else (let [saved-id (save-organizer-tournament user (update tournament :date tc/to-local-date))]
                 (response (-> (get-organizer-tournament saved-id)
                               (select-keys [:id :name :date :format :deadline])
                               (assoc :url (str "https://decklist.pairings.fi/tournament/" saved-id))))))))))
