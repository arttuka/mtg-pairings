(ns mtg-pairings-server.schema
  (:require [schema.core :as s])
  (:import org.joda.time.LocalDate
           java.util.Date))

(s/defschema ApiKeyParams {:key String})

(s/defschema BaseTournament {:id Long
                             :name String
                             :day org.joda.time.LocalDate
                             :rounds Long})

(s/defschema Tournament (merge BaseTournament 
                               {:round [Long]
                                :standings [Long]
                                :seatings Boolean}))

(s/defschema InputTournament (-> BaseTournament
                               (dissoc :id)
                               (merge {:day java.util.Date
                                       :sanctionid String
                                       :tracking Boolean})))

(s/defschema Player {:dci String
                     :name String})

(s/defschema Team {:id Long
                   :name String
                   :players [Player]})

(s/defschema InputTeams {:teams [Team]})

(s/defschema Seating {:team1_name String
                      :table_number Long})

(s/defschema InputSeating {:team String
                           :table_number Long})

(s/defschema InputSeatings {:seatings [InputSeating]} )

(s/defschema Pairing {:team1_name String
                      :team2_name String
                      :team1_points Long
                      :team2_points Long
                      :table_number Long
                      :round_number Long
                      :team1_wins (s/maybe Long)
                      :team2_wins (s/maybe Long)
                      :draws (s/maybe Long)})

(s/defschema Standing {:rank Long
                       :team_name String
                       :points Long
                       :omw Double
                       :pgw Double
                       :ogw Double})

(s/defschema InputPairing {:team1 String
                           :team2 (s/maybe String)
                           :table_number Long})

(s/defschema InputPairings {:pairings [InputPairing]})
  
(s/defschema InputResult {:team1 String
                          :team2 (s/maybe String)
                          :table_number Long
                          :team1_wins Long
                          :team2_wins Long
                          :draws Long})

(s/defschema InputResults {:results [InputResult]})

(s/defschema PlayersTournament (merge BaseTournament
                                      {:seating Seating
                                       :pairings [Pairing]
                                       :max_standings_round Long}))
