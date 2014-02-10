(ns mtg-pairings.db-reader
  (:require [clojess.core :as db]))

(defn open [filename]
  (db/open-db filename))

(defn tournaments
  "Returns a list of tournaments in the database"
  [db]
  (->> (db/table db "Tournament")
    db/rows
    (map #(select-keys % [:TournamentId :Title :StartDate]))))

(def bye
  {:id 0
   :name "***BYE***"
   :dci-numbers []})

(defn dcinumbers-for-players
  "Returns a list of DCI numbers for a list of players"
  [db players]
  (let [person-table (db/table db "Person")] 
    (for [player players
          :let [person (db/row person-table (:PersonId player))]]
      (:PrimaryDciNumber person))))

(defn teams
  "Returns a list of teams in the tournament with the given id number."
  [db tournament-id]
  (let [team-table (db/table db "Team")
        teamplayer-table (db/table db "TeamPlayers")]
    (into {} (for [team (db/rows team-table {:TournamentId tournament-id})
                   :let [teamplayers (db/rows teamplayer-table {:TeamId (:TeamId team)})]]
               {(:TeamId team) {:id (:TeamId team)
                                :name (:Name team)
                                :dci-numbers (dcinumbers-for-players db teamplayers)}}))))

(defn teams-from-ids
  [team-ids all-teams]
  (if (= 2 (count team-ids))
    (let [pair (map all-teams team-ids)]
      [pair (reverse pair)])
    [[(all-teams (first team-ids)) bye]]))

(defn results-for-round
  [db round-id all-teams]
  (let [match-table (db/table db "Match")
        result-table (db/table db "TeamMatchResult")
        matches (db/rows match-table {:RoundId round-id})]
   (for [match matches
         :let [[first-row second-row] (db/rows result-table {:MatchId (:MatchId match)})]]
     (if (>= (:GameWins first-row) (:GameWins second-row))
       {:GameWins (:GameWins first-row)
        :GameLosses (:GameLosses first-row)
        :GameDraws (:GameDraws first-row)
        :WinnerTeamId (:TeamId first-row)
        :LoserTeamId (:TeamId second-row)}
       {:GameWins (:GameWins second-row)
        :GameLosses (:GameLosses second-row)
        :GameDraws (:GameDraws second-row)
        :WinnerTeamId (:TeamId second-row)
        :LoserTeamId (:TeamId first-row)}))))
(defn pairings-for-round
  [db round-id all-teams]
  (let [match-table (db/table db "Match")
        result-table (db/table db "TeamMatchResult")
        matches (db/rows match-table {:RoundId round-id})
        results (for [match matches]
                  (map :TeamId (db/rows result-table {:MatchId (:MatchId match)})))]
    (mapcat #(teams-from-ids % all-teams) results)))

(defn pairings
  [db tournament-id]
  (let [round-table (db/table db "Round")
        rounds (db/rows round-table {:TournamentId tournament-id})
        teams (teams db tournament-id)]
    (for [round rounds]
      (pairings-for-round db (:RoundId round) teams))))

(defn print-pairings [pairings]
  (doseq [pairing pairings]
    (println (:name (first pairing)) "-" (:name (second pairing)))))
