(ns mtg-pairings-server.util.util
  (:require [#?(:clj  clj-time.format
                :cljs cljs-time.format)
             :as time]
    #?(:clj [ring.util.response :as ring])))

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
            entry (find map from)]
        (recur
          (if entry
            (conj ret [to (val entry)])
            ret)
          (next keys)))
      ret)))

(defn parse-date [date]
  (time/parse-local-date (time/formatters :year-month-day) date))

(defn format-iso-date [date]
  (time/unparse-local-date (time/formatters :year-month-day) date))

(defn format-date [date]
  (time/unparse-local-date (time/formatter "dd.MM.yyyy") date))

(defn group-kv [keyfn valfn coll]
  (apply merge-with into (for [elem coll]
                           {(keyfn elem) [(valfn elem)]})))

(defn map-by [f coll]
  (into {} (map (juxt f identity) coll)))

#?(:clj
   (defn edn-response [body]
     (if body
       {:status  200
        :headers {"Content-Type" "application/edn"}
        :body    (pr-str body)}
       (ring/not-found body))))

#?(:clj
   (defn response [body]
     (if body
       (ring/response body)
       (ring/not-found body))))
