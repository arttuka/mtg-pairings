(ns mtg-pairings-server.service.player
  (:require [honeysql.helpers :as sql]
            [mtg-pairings-server.db :as db]
            [mtg-pairings-server.util.mtg :refer [add-check-digits]]
            [mtg-pairings-server.util :refer [select-and-rename-keys]]))

(defn player [dci]
  (try
    (-> (sql/select :*)
        (sql/from :player)
        (sql/where [:= :dci (add-check-digits dci)])
        (db/query-one-or-nil))
    (catch NumberFormatException _ nil)))

(defn ^:private format-pairing
  [pairing team-id]
  (let [pairing (if (= team-id (:team1 pairing))
                  (select-keys pairing [:round_number :team1_name :team2_name :team1_points :team2_points :table_number :team1_wins :team2_wins :created])
                  (select-and-rename-keys pairing [:round_number [:team1_name :team2_name] [:team2_name :team1_name]
                                                   :table_number [:team1_points :team2_points] [:team2_points :team1_points]
                                                   [:team1_wins :team2_wins] [:team2_wins :team1_wins] :created]))]
    (if-not (:team2_name pairing)
      (merge pairing {:team2_name   "***BYE***"
                      :team2_points 0})
      pairing)))

(defn ^:private add-players-data [tournament dci]
  (let [players-team (:id (-> (sql/select :id)
                              (sql/from :team)
                              (sql/where [:= :tournament (:id tournament)]
                                         [:exists (-> (sql/select :*)
                                                      (sql/from :team_players)
                                                      (sql/where [:= :player dci]
                                                                 [:= :team :team.id]))])
                              (db/query-one)))
        pairings (for [pairing (-> (sql/select :p.* [:team1.name :team1_name] [:team2.name :team2_name] [:round.num :round_number] :round.created)
                                   (sql/from [:pairing :p])
                                   (sql/join :round [:= :p.round :round.id])
                                   (sql/merge-join [:team :team1] [:= :p.team1 :team1.id])
                                   (sql/merge-left-join [:team :team2] [:= :p.team2 :team2.id])
                                   (sql/where [:= :round.tournament (:id tournament)]
                                              [:or
                                               [:= :team1 players-team]
                                               [:= :team2 players-team]])
                                   (sql/order-by [:round.num :desc])
                                   (db/query))]
                   (format-pairing pairing players-team))
        seating (-> (sql/select :table_number [:team.name :team1_name])
                    (sql/from :seating)
                    (sql/join :team [:= :team :team.id])
                    (sql/where [:= :seating.tournament (:id tournament)]
                               [:= :team players-team])
                    (sql/order-by [:table_number :asc])
                    (sql/limit 1)
                    (db/query-one-or-nil))
        pod-seats (-> (sql/select :seat [:pod.number :pod] :pod_round.id [:num :round_number])
                      (sql/from :pod_seat)
                      (sql/join :pod [:= :pod :pod.id])
                      (sql/merge-join :pod_round [:= :pod_round :pod_round.id])
                      (sql/merge-join :round [:= :round :round.id])
                      (sql/where [:= :team players-team])
                      (sql/order-by [:pod_round.id :desc])
                      (db/query))]
    (assoc tournament
           :pairings pairings
           :seating seating
           :pod-seats pod-seats)))

(defn ^:private add-newest-standings [tournament]
  (let [standings (-> (sql/select [:%max.round :max_standings_round])
                      (sql/from :standings)
                      (sql/where [:= :tournament (:id tournament)]
                                 [:not :hidden])
                      (db/query-one-or-nil))]
    (merge tournament standings)))

(defn ^:private select-tournament-fields [tournament]
  (select-keys tournament [:id :name :day :organizer :rounds :seating :pairings :pod-seats :max_standings_round]))

(defn format-tournament [tournament dci]
  (-> tournament
      (add-players-data dci)
      add-newest-standings
      select-tournament-fields))

(defn tournaments [dci]
  (let [dci (add-check-digits dci)]
    (for [tournament (-> (sql/select :*)
                         (sql/from :tournament)
                         (sql/where [:exists (-> (sql/select :*)
                                                 (sql/from :team_players)
                                                 (sql/join :team [:= :team.id :team_players.team])
                                                 (sql/where [:= :player dci]
                                                            [:= :team.tournament :tournament.id]))])
                         (sql/order-by [:day :desc] [:id :desc])
                         (sql/limit 5)
                         (db/query))]
      (format-tournament tournament dci))))
