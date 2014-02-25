(ns mtg-pairings.util)

(defn filename
  [name]
  (fn [f]
    (= (.getName f) name)))