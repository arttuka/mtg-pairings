(ns mtg-pairings-server.schema
  (:require [schema.core :as s]
            [ring.swagger.schema :refer :all]))

(defmodel BaseTournament {:id Long
                          :name String
                          :day String
                          :rounds Long})

(defmodel Tournament (assoc BaseTournament :round [Long]
                                           :standings [Long]
                                           :seatings Boolean))

(defmodel InputTournament (-> BaseTournament
                            (dissoc :id)
                            (assoc :day String)))

(defmodel Player {:dci String
                  :name String})

(defmodel Team {:name String
                :players [Player]})

(defmodel InputTeams {:teams [Team]})

(defmodel Seating {:team1_name String
                   :table_number Long})

(defmodel InputSeating {:name String
                        :table_number Long})

(defmodel InputSeatings {:seatings [InputSeating]} )

(defmodel Pairing {:team1_name String
                   (s/optional-key :team2_name) String
                   :team1_points Long
                   (s/optional-key :team2_points) Long
                   :table_number Long
                   (s/optional-key :round_number) Long
                   (s/optional-key :team1_wins) Long
                   (s/optional-key :team2_wins) Long
                   (s/optional-key :draws) Long})

(defmodel Standing {:rank Long
                    :team String
                    :points Long
                    :omw Double
                    :pgw Double
                    :ogw Double})

(defmodel InputPairing {:team1 String
                        :team2 String
                        :table_number Long})

(defmodel InputPairings {:pairings [InputPairing]})
  
(defmodel InputResult {:team1 String
                       :team2 String
                       :table_number Long
                       :team1_wins Long
                       :team2_wins Long
                       :draws Long})

(defmodel InputResults {:results [InputResult]})

(defmodel PlayersTournament (merge BaseTournament
                                   {:seating Seating
                                    :pairings [Pairing]}))
