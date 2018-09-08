(ns mtg-pairings-server.service.player
  (:require [korma.core :as sql]
            [mtg-pairings-server.sql-db :as db]
            [mtg-pairings-server.util.mtg-util :refer [add-check-digits]]
            [mtg-pairings-server.util.sql :as sql-util]
            [mtg-pairings-server.util.util :refer [select-and-rename-keys]]))

(defn player [dci]
  (sql-util/select-unique-or-nil db/player
    (sql/where {:dci (add-check-digits dci)})))

(defn players []
  (sql/select db/player))

(defn ^:private format-pairing
  [pairing team-id]
  (let [pairing (if (= team-id (:team1 pairing))
                  (select-keys pairing [:round_number :team1_name :team2_name :team1_points :team2_points :table_number :team1_wins :team2_wins])
                  (select-and-rename-keys pairing [:round_number [:team1_name :team2_name] [:team2_name :team1_name]
                                                   :table_number [:team1_points :team2_points] [:team2_points :team1_points]
                                                   [:team1_wins :team2_wins] [:team2_wins :team1_wins]]))]
    (if-not (:team2_name pairing)
      (merge pairing {:team2_name   "***BYE***"
                      :team2_points 0})
      pairing)))

(defn ^:private add-players-data [tournament dci]
  (let [players-team (:id (sql-util/select-unique-or-nil db/team
                            (sql/where (and {:tournament (:id tournament)}
                                            (sql/sqlfn "exists"
                                              (sql/subselect db/team-players
                                                (sql/where {:team_players.player dci
                                                            :team_players.team   :team.id})))))))
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
        seating (sql-util/select-unique-or-nil db/seating
                  (sql/with db/team)
                  (sql/where {:tournament (:id tournament)
                              :team       players-team})
                  (sql/fields :table_number [:team.name :team1_name]))
        pod-seats (sql/select db/seat
                    (sql/where {:team players-team})
                    (sql/fields :seat)
                    (sql/with db/pod
                      (sql/fields [:number :pod])
                      (sql/with db/pod-round
                        (sql/fields :id)
                        (sql/with db/round
                          (sql/fields [:num :round_number]))))
                    (sql/order :pod_round.id :DESC))]
    (assoc tournament :pairings pairings
                      :seating seating
                      :pod-seats pod-seats)))

(defn ^:private add-newest-standings [tournament]
  (let [standings (sql-util/select-unique-or-nil db/standings
                    (sql/aggregate (max :round) :max_standings_round)
                    (sql/where {:tournament (:id tournament)
                                :hidden     false}))]
    (merge tournament standings)))

(defn ^:private select-tournament-fields [tournament]
  (select-keys tournament [:id :name :day :rounds :seating :pairings :pod-seats :max_standings_round]))

(defn format-tournament [tournament dci]
  (-> tournament
    (add-players-data dci)
    add-newest-standings
    select-tournament-fields))

(defn tournaments [dci]
  (let [dci (add-check-digits dci)]
    (for [tournament (sql/select db/tournament
                       (sql/where (sql/sqlfn "exists"
                                    (sql/subselect db/team-players
                                      (sql/join db/team (= :team.id :team_players.team))
                                      (sql/where {:team_players.player dci
                                                  :team.tournament     :tournament.id}))))
                       (sql/order :day :DESC)
                       (sql/order :id :DESC))]
      (format-tournament tournament dci))))
