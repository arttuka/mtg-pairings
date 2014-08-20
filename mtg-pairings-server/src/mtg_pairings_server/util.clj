(ns mtg-pairings-server.util
  (:require [ring.util.response :as ring]
            [clj-time.format :as time]))

(defn map-values
  "Returns a map consisting of the keys of m mapped to 
the result of applying f to the value of that key in m.
Function f should accept one argument."
  [f m]
  (into {}
    (for [[k v] m]
      [k (f v)])))

(defn some-value [pred coll]
  (first (filter pred coll)))

(defn select-and-rename-keys
  [map keys]
  (loop [ret {} keys (seq keys)]
    (if keys
      (let [key (first keys)
            [from to] (if (coll? key)
                        key
                        [key key])
            entry (. clojure.lang.RT (find map from))]
        (recur
          (if entry
            (conj ret [to (val entry)])
            ret)
          (next keys)))
      ret)))

(defn edn-response [body]
  (if body
    {:status 200
     :headers {"Content-Type" "application/edn"}
     :body (pr-str body)}
    (ring/not-found body)))

(defn response [body]
  (if body
    (ring/response body)
    (ring/not-found body)))

(defn parse-date [date]
  (time/parse-local-date (time/formatters :year-month-day) date))
