(ns mtg-pairings-server.util.mtg
  (:require [clojure.set :refer [rename-keys]]
            [mtg-pairings-server.util :refer [map-values]]))

(defn ^:private add-result [acc {:keys [team1_wins team2_wins draws]}]
  (let [result {:match-points   (cond
                                  (= team1_wins team2_wins) 1
                                  (> team1_wins team2_wins) 3
                                  :else 0)
                :game-points    (+ (* 3 team1_wins)
                                   (* 1 draws))
                :games-played   (+ team1_wins draws team2_wins)
                :matches-played 1}]
    (merge-with + acc result)))

(defn ^:private calculate-points-pgw [matches]
  (let [{:keys [match-points game-points matches-played games-played]}
        (reduce add-result {:match-points 0, :game-points 0, :games-played 0, :matches-played 0} matches)]
    {:points    match-points
     :pmw       (max (/ 33 100) (/ match-points (* 3 matches-played)))
     :pgw       (max (/ 33 100) (/ game-points (* 3 games-played)))
     :opponents (keep :team2 matches)
     :team      (:team1 (first matches))
     :team_name (:team1_name (first matches))}))

(defn ^:private avg-of [key coll]
  (if (seq coll)
    (/ (transduce (map key) + 0 coll) (count coll))
    0))

(defn ^:private calculate-omw-ogw [teams-results]
  (for [[_ results] teams-results
        :let [opponents (map teams-results (:opponents results))]]
    (assoc results
           :omw (avg-of :pmw opponents)
           :ogw (avg-of :pgw opponents))))

(defn reverse-match [match]
  (rename-keys match {:team1        :team2
                      :team2        :team1
                      :team1_name   :team2_name
                      :team2_name   :team1_name
                      :team1_wins   :team2_wins
                      :team2_wins   :team1_wins
                      :team1_points :team2_points
                      :team2_points :team1_points
                      :draws        :draws}))

(defn has-result? [match]
  (some? (:team1_wins match)))

(defn calculate-standings [rounds up-to-round-num]
  (let [matches (for [[round-num matches] rounds
                      :when (<= round-num up-to-round-num)
                      match matches
                      :when (has-result? match)
                      duplicated [match (reverse-match match)]]
                  duplicated)
        grouped-matches (dissoc (group-by :team1 matches) nil)
        teams-results (map-values grouped-matches calculate-points-pgw)
        results (calculate-omw-ogw teams-results)
        sorted (reverse (sort-by (juxt :points :omw :pgw :ogw) results))]
    (map-indexed #(assoc %2 :rank (inc %1)) sorted)))

(defn ^:private check-digit [n]
  (let [primes [43 47 53 71 73 31 37 41 59 61 67 29]
        checksum (reduce + (map * n primes))]
    (-> checksum
        (quot 10)
        (mod 9)
        inc)))

(defn add-check-digits [dci-number]
  (let [digits (map #(#?(:clj Integer/parseInt, :cljs js/parseInt) (str %)) dci-number)
        length (count digits)]
    (cond->> digits
      (>= 6 length) (cons 0)
      (>= 7 length) (#(cons (check-digit %) %))
      (>= 8 length) (cons 0)
      (>= 9 length) (#(cons (check-digit %) %))
      true (apply str))))

(defn duplicate-pairings [pairings]
  (->> pairings
       (concat (map reverse-match pairings))
       (remove #(= "***BYE***" (:team1_name %)))))

(defn valid-dci? [dci-number]
  (when dci-number
    (re-matches #"[0-9]+" dci-number)))

(defn bye? [pairing]
  (zero? (or (:table_number pairing)
             (:table-number pairing))))
