(ns mtg-pairings.db-reader
  (:require [clojess.core :as db]))

(defn open [filename]
  (db/open-db filename))

(defn tournaments
  [db]
  (->> (db/table db "Tournament")
    db/rows
    (map #(select-keys % [:TournamentId :Title :StartDate]))))

(def bye
  {:id 0
   :name "***BYE***"
   :dci-numbers []})

(defn teams
  [db tournament-id]
  (let [person-table (db/table db "Person")
        team-table (db/table db "Team")
        teamplayer-table (db/table db "TeamPlayers")]
    (into {} (for [team (db/rows team-table {:TournamentId tournament-id})
                   :let [teamplayers (db/rows teamplayer-table {:TeamId (:TeamId team)})]]
               [(:TeamId team) {:id (:TeamId team)
                                :name (:Name team)
                                :dci-numbers (for [teamplayer teamplayers
                                                   :let [person (db/row person-table (:PersonId teamplayer))]]
                                               (:PrimaryDciNumber person))}]))))

(defn teams-from-results
  [results teams]
  (if (= 2 (count results))
    (let [pair (for [result results]
                 (teams (:TeamId result)))]
      [pair (reverse pair)])
    [(teams (:TeamId (first results))) bye]))

(defn pairings-for-round
  [db round-id teams]
  (let [match-table (db/table db "Match")
        result-table (db/table db "TeamMatchResult")]
    (apply concat (for [match (db/rows match-table {:RoundId round-id})
                        :let [results (db/rows result-table {:MatchId (:MatchId match)})]]
                    (teams-from-results results teams)))))

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
  