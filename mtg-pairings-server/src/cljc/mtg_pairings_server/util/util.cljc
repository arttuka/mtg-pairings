(ns mtg-pairings-server.util.util
  (:require [#?(:clj  clj-time.format
                :cljs cljs-time.format)
             :as format]
            [#?(:clj  clj-time.core
                :cljs cljs-time.core)
             :as time]
    #?(:clj
            [ring.util.response :as ring])
    #?@(:cljs
        [[goog.string :as gstring]
         [goog.string.format]])
            [clojure.string :as str]))

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
  (format/parse-local-date (format/formatters :year-month-day) date))

(defn format-iso-date [date]
  (format/unparse-local-date (format/formatters :year-month-day) date))

(defn format-date [date]
  (some->> date (format/unparse-local-date (format/formatter "dd.MM.yyyy"))))

(defn today-or-yesterday? [date]
  (let [yesterday (time/minus (time/today) (time/days 1))
        tomorrow (time/plus (time/today) (time/days 1))]
    (time/within? (time/interval yesterday tomorrow) date)))

(defn group-kv [keyfn valfn coll]
  (apply merge-with into (for [elem coll]
                           {(keyfn elem) [(valfn elem)]})))

(defn extract-list [k coll]
  (for [[m v] (group-kv #(dissoc % k) #(get % k) coll)]
    (assoc m k v)))

(defn map-by [f coll]
  (into {} (map (juxt f identity) coll)))

(defn indexed [coll]
  (map-indexed vector coll))

(defn assoc-in-many [m & kvs]
  (reduce (fn [m [ks v]] (assoc-in m ks v)) m (partition 2 kvs)))

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

(defn cls [class-defs]
  (str/join " " (for [[c used?] class-defs
                      :when used?]
                  (name c))))

#?(:cljs
   (defn round [num]
     (if (neg? num)
       (Math/ceil num)
       (Math/floor num))))

#?(:cljs
   (defn format-time [seconds]
     (let [sign (if (neg? seconds) "-" "")
           minutes (Math/abs (round (/ seconds 60)))
           seconds (mod (Math/abs seconds) 60)]
       (gstring/format "%s%02d:%02d" sign minutes seconds))))
