(ns mtg-pairings.util
  (:require [clj-time.core :as time]
            [clojure.string :as string]))

(defn filename
  [name]
  (fn [f]
    (= (.getName f) name)))

(defn zip [& colls]
  (apply map vector colls))

(defn sanction-id [tournament]
  (if-not (string/blank? (:SanctionId tournament))
    (:SanctionId tournament)
    (str (:TournamentId tournament) "-" (:StartDate tournament))))
