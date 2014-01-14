(ns mtg-pairings-backend.mtg-util
  (:require [mtg-pairings-backend.util :refer [map-values]]))

(defn ^:private add-result [acc res]
  (let [result {:match-points (cond
                                (= (:wins res) (:losses res)) 1
                                (> (:wins res) (:losses res)) 3
                                :else 0)
               :game-points (+ (* 3 (:wins res))
                               (* 1 (:draws res)))
               :games-played (+ (:wins res) (:draws res) (:losses res))
               :matches-played 1}]
    (merge-with + acc result)))

(defn ^:private calculate-points-pgw [matches]
  (let [reduced (reduce add-result {:match-points 0, :game-points 0, :games-played 0, :matches-played 0} matches)]
    {:points (:match-points reduced)
     :pmw (max 0.33 (/ (:match-points reduced) (* 3 (:matches-played reduced))))
     :pgw (max 0.33 (/ (:game-points reduced) (* 3 (:games-played reduced))))
     :opponents (keep :team-2 matches)}))

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
  {:team-1 (:team-2 match)
   :team-2 (:team-1 match)
   :wins (:losses match)
   :losses (:wins match)
   :draws (:draws match)})

(defn calculate-standings [rounds up-to-round-num]
  (let [matches (apply concat (for [[round-num matches] rounds
                                    :when (<= round-num up-to-round-num)]
                                matches))
        all-matches (concat matches (map reverse-match matches))
        grouped-matches (group-by :team-1 all-matches)
        teams-results (map-values calculate-points-pgw grouped-matches)
        results (calculate-omw-ogw teams-results)
        lst (for [[team result] results]
              (assoc result :team team))
        sorted (reverse (sort-by (juxt :points :omw :pgw :ogw) lst))]
    (map (fn [idx res] (assoc res :rank idx)) (iterate inc 1) sorted)))
