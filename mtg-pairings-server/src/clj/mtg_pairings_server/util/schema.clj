(ns mtg-pairings-server.util.schema
  (:require [schema.core :as s])
  (:import org.joda.time.LocalDate
           java.util.Date))

(s/defschema BaseTournament {:id        Long
                             :name      String
                             :organizer (s/maybe String)
                             :day       LocalDate
                             :rounds    Long})

(s/defschema Tournament (merge BaseTournament
                               {:pairings  [Long]
                                :results   [Long]
                                :standings [Long]
                                :pods      [Long]
                                :seatings  Boolean
                                :playoff   Boolean
                                :players   Long}))

(s/defschema InputTournament (-> BaseTournament
                                 (dissoc :id)
                                 (merge {:day                       Date
                                         :sanctionid                String
                                         (s/optional-key :tracking) Boolean})))

(s/defschema Player {:dci  String
                     :name String})

(s/defschema Team {(s/optional-key :id) Long
                   :name                String
                   :players             [Player]})

(s/defschema InputTeam [String])

(s/defschema InputTeams {:teams [Team]})

(s/defschema Seating {:team1_name   String
                      :table_number Long})

(s/defschema InputSeating {:team         InputTeam
                           :table_number Long})

(s/defschema InputSeatings {:seatings [InputSeating]})

(s/defschema Pairing {:team1_name   String
                      :team2_name   String
                      :team1_points Long
                      :team2_points Long
                      :table_number Long
                      :round_number Long
                      :team1_wins   (s/maybe Long)
                      :team2_wins   (s/maybe Long)
                      :draws        (s/maybe Long)})

(s/defschema Standing {:rank      Long
                       :team_name String
                       :points    Long
                       :omw       Double
                       :pgw       Double
                       :ogw       Double})

(s/defschema InputPairing {:team1        InputTeam
                           :team2        (s/maybe [String])
                           :table_number Long})

(s/defschema InputPairings {:pairings [InputPairing]
                            :playoff  Boolean})

(s/defschema InputResult {:team1        InputTeam
                          :team2        (s/maybe InputTeam)
                          :table_number Long
                          :team1_wins   Long
                          :team2_wins   Long
                          :draws        Long})

(s/defschema InputResults {:results [InputResult]})

(s/defschema InputPodSeat {:seat s/Int
                           :team InputTeam})

(s/defschema InputPod {:number s/Int
                       :seats  [InputPodSeat]})

(s/defschema InputPodRound {:pods  [InputPod]
                            :round s/Int})
