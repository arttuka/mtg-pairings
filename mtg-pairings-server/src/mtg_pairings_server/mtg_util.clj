(ns mtg-pairings-server.mtg-util
  (:require [mtg-pairings-server.util :refer [map-values]]))

(defn ^:private add-result [acc res]
  (let [result {:match_points (cond
                                (= (:wins res) (:losses res)) 1
                                (> (:wins res) (:losses res)) 3
                                :else 0)
               :game_points (+ (* 3 (:wins res))
                               (* 1 (:draws res)))
               :games_played (+ (:wins res) (:draws res) (:losses res))
               :matches_played 1}]
    (merge-with + acc result)))

(defn ^:private calculate-points-pgw [matches]
  (let [reduced (reduce add-result {:match_points 0, :game_points 0, :games_played 0, :matches_played 0} matches)]
    {:points (:match_points reduced)
     :pmw (max 0.33 (/ (:match_points reduced) (* 3 (:matches_played reduced))))
     :pgw (max 0.33 (/ (:game_points reduced) (* 3 (:games_played reduced))))
     :opponents (keep :team_2 matches)
     :team_name (:team_1_name (first matches))}))

(defn ^:private calculate-omw-ogw [teams-results]
  (map-values (fn [results] 
                (let [opponents (for [opp (:opponents results)]
                                  (teams-results opp))
                      cnt (count opponents)
                      omw (/ (reduce + 0 (map :pmw opponents)) cnt)
                      ogw (/ (reduce + 0 (map :pgw opponents)) cnt)]
                  (assoc results
                         :omw omw
                         :ogw ogw)))
              teams-results))

(defn ^:private reverse-match [match]
  {:team_1 (:team_2 match)
   :team_2 (:team_1 match)
   :team_1_name (:team_2_name match)
   :team_2_name (:team_1_name match)
   :wins (:losses match)
   :losses (:wins match)
   :draws (:draws match)})

(defn calculate-standings [rounds up-to-round-num]
  (let [matches (apply concat (for [[round-num matches] rounds
                                    :when (<= round-num up-to-round-num)]
                                matches))
        all-matches (concat matches (map reverse-match matches))
        grouped-matches (group-by :team_1 all-matches)
        teams-results (map-values calculate-points-pgw grouped-matches)
        results (calculate-omw-ogw teams-results)
        lst (for [[team result] results]
              (assoc result :team team))
        sorted (reverse (sort-by (juxt :points :omw :pgw :ogw) lst))]
    (map (fn [idx res] (assoc res :rank idx)) (iterate inc 1) sorted)))
