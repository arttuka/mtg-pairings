(ns mtg-pairings-server.mtg-util
  (:require [mtg-pairings-server.util :refer [map-values]]))

(defn ^:private add-result [acc res]
  (let [result {:match_points (cond
                                (= (:team1_wins res) (:team2_wins res)) 1
                                (> (:team1_wins res) (:team2_wins res)) 3
                                :else 0)
               :game_points (+ (* 3 (:team1_wins res))
                               (* 1 (:draws res)))
               :games_played (+ (:team1_wins res) (:draws res) (:team2_wins res))
               :matches_played 1}]
    (merge-with + acc result)))

(defn ^:private calculate-points-pgw [matches]
  (let [reduced (reduce add-result {:match_points 0, :game_points 0, :games_played 0, :matches_played 0} matches)]
    {:points (:match_points reduced)
     :pmw (max 0.33 (/ (:match_points reduced) (* 3 (:matches_played reduced))))
     :pgw (/ (:game_points reduced) (* 3 (:games_played reduced)))
     :opponents (keep :team2 matches)
     :team_name (:team1_name (first matches))}))

(defn ^:private calculate-omw-ogw [teams-results]
  (map-values (fn [results]
                (let [opponents (for [opp (:opponents results)]
                                  (teams-results opp))
                      cnt (count opponents)
                      omw (if (zero? cnt)
                            0
                            (/ (reduce + 0 (map :pmw opponents)) cnt))
                      ogw (if (zero? cnt)
                            0
                            (/ (reduce + 0 (map :pgw opponents)) cnt))]
                  (assoc results
                         :omw omw
                         :ogw ogw)))
              teams-results))

(defn ^:private reverse-match [match]
  {:team1 (:team2 match)
   :team2 (:team1 match)
   :team1_name (:team2_name match)
   :team2_name (:team1_name match)
   :team1_wins (:team2_wins match)
   :team2_wins (:team1_wins match)
   :draws (:draws match)})

(defn calculate-standings [rounds up-to-round-num]
  (let [matches (apply concat (for [[round-num matches] rounds
                                    :when (<= round-num up-to-round-num)]
                                matches))
        all-matches (concat matches (map reverse-match matches))
        grouped-matches (dissoc (group-by :team1 all-matches) nil)
        teams-results (map-values calculate-points-pgw grouped-matches)
        results (calculate-omw-ogw teams-results)
        lst (for [[team result] results]
              (assoc result :team team))
        sorted (reverse (sort-by (juxt :points :omw :pgw :ogw) lst))]
    (map (fn [idx res] (assoc res :rank idx)) (iterate inc 1) sorted)))

(defn ^:private check-digit [n]
  (let [primes [43 47 53 71 73 31 37 41 59 61 67 29]
        checksum (reduce + (map * n primes))]
    (-> checksum
      (/ 10)
      int
      (mod 9)
      (+ 1))))

(defn add-check-digits [dci-number]
  (let [digits (map #(Integer/parseInt (str %)) dci-number)
        length (count digits)]
    (cond->> digits
      (>= 6 length) (cons 0)
      (>= 7 length) (#(cons (check-digit %) %))
      (>= 8 length) (cons 0)
      (>= 9 length) (#(cons (check-digit %) %))
      true (apply str))))
