(ns mtg-pairings.db-reader
  (:require [clojess.core :as db]
            [clj-time.coerce :as coerce]
            [mtg-pairings.util :as util]))

(defn open [filename]
  (db/open-db filename))

(defn tournaments
  "Returns a list of tournaments in the database matching the given criteria"
  [db criteria]
  (doall (as-> (db/table db "Tournament") <>
           (db/rows <> criteria)
           (map #(select-keys % [:TournamentId :Title :StartDate :NumberOfRounds :SanctionId]) <>)
           (map #(update-in % [:StartDate] coerce/to-local-date) <>))))

(def bye
  {:id 0
   :name "***BYE***"
   :dci-numbers []})

(defn data-for-players
  "Returns a list of DCI numbers for a list of players"
  [db players]
  (let [person-table (db/table db "Person")]
    (doall (for [player players
                 :let [person (db/row person-table (:PersonId player))]]
             {:dci (:PrimaryDciNumber person)
              :name (str (:LastName person) ", " (:FirstName person)
                         (when (seq (:MiddleInitial person))
                           (str " " (:MiddleInitial person))))}))))

(defn teams
  [db tournament-id]
  (let [team-table (db/table db "Team")
        teamplayer-table (db/table db "TeamPlayers")]
    (doall (for [team (db/rows team-table {:TournamentId tournament-id})
                 :let [teamplayers (db/rows teamplayer-table {:TeamId (:TeamId team)})]]
             {:id (:TeamId team)
              :name (:Name team)
              :players (data-for-players db teamplayers)}))))

(defn id->team
  "Returns a map of team-id -> team in the tournament with the given id number."
  [db tournament-id]
  (into {} (for [team (teams db tournament-id)]
             [(:id team) team])))

(defn ^:private wins [row]
  (if (:IsBye row)
    2
    (:GameWins row)))

(defn ^:private losses [row]
  (if (:IsBye row)
    0
    (:GameLosses row)))

(defn ^:private draws [row]
  (if (:IsBye row)
    0
    (:GameDraws row)))

(defn results-for-round
  [db round-id all-teams]
  (let [match-table (db/table db "Match")
        result-table (db/table db "TeamMatchResult")
        matches (db/rows match-table {:RoundId round-id})]
   (doall (for [match matches
                :let [[first-row second-row] (db/rows result-table {:MatchId (:MatchId match)})]]
            (merge {:table_number (:TableNumber match)}
                   (if (or (nil? second-row) (> (:TeamId first-row) (:TeamId second-row)))
                     {:team1_wins (wins first-row)
                      :team2_wins (losses first-row)
                      :draws (draws first-row)
                      :team1 (:name (all-teams (:TeamId first-row)))
                      :team2 (:name (all-teams (:TeamId second-row)))}
                     {:team1_wins (wins second-row)
                      :team2_wins (losses second-row)
                      :draws (draws second-row)
                      :team1 (:name (all-teams (:TeamId second-row)))
                      :team2 (:name (all-teams (:TeamId first-row)))}))))))
(defn pairings-for-round
  "Returns a list of pairings for the round so that team1 is the one with the higher id"
  [db round-id all-teams]
  (let [match-table (db/table db "Match")
        result-table (db/table db "TeamMatchResult")
        matches (db/rows match-table {:RoundId round-id})]
    (doall (for [match matches
                 :let [match-teams (map :TeamId (db/rows result-table {:MatchId (:MatchId match)}))
                       teams (reverse (sort match-teams))
                       team1 (first teams)
                       team2 (second teams)]]
             {:team1 (:name (all-teams team1))
              :team2 (:name (all-teams team2))
              :table_number (:TableNumber match)}))))

(defn rounds
  [db tournament-id]
  (doall (as-> (db/table db "Round") <>
           (db/rows <> {:TournamentId tournament-id})
           (map #(select-keys % [:RoundId :Number]) <>))))

(defn results
  [db tournament-id]
  (let [round-table (db/table db "Round")
        rounds (sort-by :Number (db/rows round-table {:TournamentId tournament-id}))
        teams (id->team db tournament-id)]
    (into {} (for [round rounds
                   :let [results (results-for-round db (:RoundId round) teams)]
                   :when (seq results)]
               [(:Number round) results]))))

(defn pairings
  [db tournament-id]
  (let [round-table (db/table db "Round")
        rounds (sort-by :Number (db/rows round-table {:TournamentId tournament-id}))
        teams (id->team db tournament-id)]
    (into {} (for [round rounds
                   :let [pairings (pairings-for-round db (:RoundId round) teams)]
                   :when (seq pairings)]
               [(:Number round) pairings]))))

(defn seatings
  [db tournament-id]
  (let [tournamenttable-table (db/table db "TournamentTable")
        seat-table (db/table db "Seat")
        teams (id->team db tournament-id)
        tables (db/rows tournamenttable-table {:TournamentId tournament-id})]
    (doall (for [table tables
                 seat (db/rows seat-table {:TournamentTableId (:TournamentTableId table)})]
             {:table_number (:TableNumber table)
              :team (-> seat :TeamId teams :name)}))))
