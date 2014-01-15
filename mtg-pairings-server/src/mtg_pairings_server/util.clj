(ns mtg-pairings-server.util)

(defn map-values
  "Returns a map consisting of the keys of m mapped to 
the result of applying f to the value of that key in m.
Function f should accept one argument."
  [f m]
  (into {}
    (for [[k v] m]
      [k (f v)])))
