(ns mtg-pairings.util
  (:require [clj-time.core :as time]
            [clojure.string :as string]))

(defn filename
  [name]
  (fn [f]
    (= (.getName f) name)))

(defn zip [& colls]
  (let [firsts (mapv first colls)
        rests (map rest colls)]
    (lazy-seq
      (when (not-every? nil? firsts)
        (cons firsts (apply zip rests))))))

(defn sanction-id [tournament]
  (if-not (string/blank? (:SanctionId tournament))
    (:SanctionId tournament)
    (str (:TournamentId tournament) "-" (:StartDate tournament))))
