(ns mtg-pairings-server.players
  (:require [korma.core :as sql]
            [mtg-pairings-server.sql-db :as db]
            [mtg-pairings-server.util :refer [select-and-rename-keys]]))

(defn player [dci]
  (first
    (sql/select db/player
      (sql/where {:dci dci}))))

(defn players []
  (sql/select db/player))

(defn ^:private format-pairing
  [pairing team-id]
  (if (= team-id (:team1 pairing))
    (select-keys pairing [:round_number :team1_name :team2_name :team1_points :team2_points :table_number :team1_wins :team2_wins])
    (select-and-rename-keys pairing [:round_number [:team1_name :team2_name] [:team2_name :team1_name]
                                     :table_number [:team1_points :team2_points] [:team2_points :team1_points]
                                     [:team1_wins :team2_wins] [:team2_wins :team1_wins]])))

(defn ^:private add-players-data [tournament dci]
  (let [players-team (:id (first (sql/select db/team
                        (sql/where (and {:tournament (:id tournament)}
                                        (sql/sqlfn exists
                                          (sql/subselect db/team-players
                                            (sql/where {:team_players.player dci
                                                        :team_players.team :team.id}))))))))
        pairings (for [pairing (sql/select db/pairing
                                 (sql/with db/team1
                                   (sql/fields [:name :team1_name]))
                                 (sql/with db/team2
                                   (sql/fields [:name :team2_name]))
                                 (sql/with db/round
                                   (sql/fields [:num :round_number]))
                                 (sql/with db/result
                                   (sql/fields :team1_wins :team2_wins))
                                 (sql/where (and {:round.tournament (:id tournament)}
                                                 (or {:team1 players-team}
                                                     {:team2 players-team})))
                                 (sql/order :round.num :DESC))]
                   (format-pairing pairing players-team))
        seating (first (sql/select db/seating
                         (sql/with db/team)
                         (sql/where {:tournament (:id tournament)
                                     :team players-team})
                         (sql/fields :table_number [:team.name :team1_name])))]
    (assoc tournament :pairings pairings
                      :seating seating)))

(defn tournaments [dci]
  (for [tournament (sql/select db/tournament
                     (sql/where (sql/sqlfn exists 
                                  (sql/subselect db/team-players
                                    (sql/join db/team (= :team.id :team_players.team))
                                    (sql/where {:team_players.player dci
                                                :team.tournament :tournament.id})))))]
    (add-players-data tournament dci)))
